# syntax=docker/dockerfile:1
# Image for inventory-service. Same structure as services/product-service/Dockerfile, which explains each step.
#
# Build context = the REPO ROOT, because the image also needs libs/stockline-common.
# Normal use (from infra/):  docker compose --profile services up -d --build
# By hand (from the repo root): docker build -f services/inventory-service/Dockerfile -t stockline/inventory-service .

# ---------------------------------------------------------------------------------------------------------------
# Stage 1: build the jar
# ---------------------------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# The library first, so a change in the service alone reuses the cached library install
COPY libs/stockline-common/pom.xml libs/stockline-common/mvnw libs/stockline-common/
COPY libs/stockline-common/.mvn libs/stockline-common/.mvn
COPY libs/stockline-common/src libs/stockline-common/src

# Tests are skipped: they use Testcontainers, and there is no Docker daemon inside "docker build"
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	cd libs/stockline-common \
	&& chmod +x mvnw \
	&& ./mvnw -B install -DskipTests

COPY services/inventory-service/pom.xml services/inventory-service/mvnw services/inventory-service/
COPY services/inventory-service/.mvn services/inventory-service/.mvn
COPY services/inventory-service/src services/inventory-service/src

# Build the jar and split it into layers (dependencies, loader, snapshot dependencies, application)
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	cd services/inventory-service \
	&& chmod +x mvnw \
	&& ./mvnw -B package -DskipTests \
	&& cp target/inventory-service-*.jar /workspace/app.jar \
	&& cd /workspace \
	&& java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------------------------------------------------------------------------------------------------------------
# Stage 2: the image that runs
# ---------------------------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

# Run as a normal user, not root
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

WORKDIR /app

# From "changes rarely" to "changes on every build", so a code change only rebuilds the last layer
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring

ENV TZ=UTC
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Documentation only: the port the app listens on (SERVER_PORT). Publishing it is done in docker-compose.yml.
# JWT_SECRET is never baked in here; it comes from the environment at runtime.
EXPOSE 8083

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
