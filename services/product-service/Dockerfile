# syntax=docker/dockerfile:1
# The line above picks the Dockerfile syntax version; it enables BuildKit features such as RUN --mount (cache mounts).

# Image for product-service.
#
# Build context = the REPO ROOT (not this folder), because the image also needs libs/stockline-common.
# Normal use (from infra/):  docker compose --profile services up -d --build
# By hand (from the repo root): docker build -f services/product-service/Dockerfile -t stockline/product-service .
#
# Multi-stage build: each FROM starts a new stage. The "build" stage has the full JDK and Maven and compiles the
# code; the final stage starts again from a small JRE image and copies only the finished application into it.
# Maven, the source code and ~/.m2 never end up in the image you run.

# ---------------------------------------------------------------------------------------------------------------
# Stage 1: build the jar
# ---------------------------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Order matters: Docker reuses a step's cached result only if that step AND every step before it are unchanged.
# Once one COPY brings in a changed file, every later step runs again. So the library is copied and installed
# first, and the service's files are copied only after that: a change in the service alone reuses the cached
# library install and rebuilds just the service steps.
#
# Only what Maven needs (pom.xml, wrapper, sources); .dockerignore keeps target/, IDE files, etc. out anyway.
COPY libs/stockline-common/pom.xml libs/stockline-common/mvnw libs/stockline-common/
COPY libs/stockline-common/.mvn libs/stockline-common/.mvn
COPY libs/stockline-common/src libs/stockline-common/src

# Tests are skipped here on purpose (-DskipTests): the service's tests use Testcontainers, which needs a Docker
# daemon, and there is none inside "docker build". The tests run with ./mvnw test before every commit (CLAUDE.md).
#
# --mount=type=cache keeps /root/.m2 (Maven's local repository, plus the Maven version the wrapper downloads)
# between builds, outside the image. Dependencies are downloaded once, not on every build. Both RUNs use the same
# cache target, so the stockline-common jar installed by the first RUN is found by the second. sharing=locked:
# when several service images build at the same time, they take turns instead of writing the same jar at once.
#
# chmod: the files come from a Windows checkout and may not carry the Unix "executable" bit.

# 1. Install the shared library into the (cached) local Maven repository
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	cd libs/stockline-common \
	&& chmod +x mvnw \
	&& ./mvnw -B install -DskipTests

# The service's files, after the library install (see "Order matters" above)
COPY services/product-service/pom.xml services/product-service/mvnw services/product-service/
COPY services/product-service/.mvn services/product-service/.mvn
COPY services/product-service/src services/product-service/src

# 2. Build the service jar, then split it into layers (see the runtime stage).
#    -Djarmode=tools starts the jar in a special mode that unpacks it instead of running the app:
#      --layers    one folder per layer listed in BOOT-INF/layers.idx
#      --launcher  keep Spring Boot's launcher classes, so the app starts with JarLauncher as from the jar
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	cd services/product-service \
	&& chmod +x mvnw \
	&& ./mvnw -B package -DskipTests \
	&& cp target/product-service-*.jar /workspace/app.jar \
	&& cd /workspace \
	&& java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------------------------------------------------------------------------------------------------------------
# Stage 2: the image that runs
# ---------------------------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

# Run as a normal user, not root: if someone breaks into the app, they don't get root inside the container.
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

WORKDIR /app

# Docker caches each COPY as its own image layer and only rebuilds (and re-downloads, on a pull) the layers whose
# files changed. So copy from "changes rarely" to "changes on every build":
#   dependencies           third-party jars (Spring, Hibernate, MySQL driver, ...)  change when pom.xml changes
#   spring-boot-loader     Spring Boot's launcher classes                           change with the Boot version
#   snapshot-dependencies  *-SNAPSHOT jars, i.e. our stockline-common               change when the library changes
#   application            this service's classes, application.yml, migrations     change on every build
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring

# Same zone as MySQL; the app works in UTC anyway (Instant + Clock, hibernate.jdbc.time_zone: UTC)
ENV TZ=UTC
# Let the heap use up to 75% of the container's memory limit (the JVM default is only 25%)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Documentation only: the port the app listens on (SERVER_PORT). Publishing it is done in docker-compose.yml.
EXPOSE 8082

# Exec form (a JSON array): java runs as process 1 and receives "docker stop" (SIGTERM) directly, so Spring Boot
# shuts down gracefully. JarLauncher reads BOOT-INF/classes and BOOT-INF/lib from the current folder (/app).
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
