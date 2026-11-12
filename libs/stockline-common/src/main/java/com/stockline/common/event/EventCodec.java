package com.stockline.common.event;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns an {@link EventEnvelope} into the JSON text a Kafka message carries, and back.
 *
 * <p>Plain JSON text instead of Spring Kafka's JSON serializer: that one adds the Java class name of the payload as a
 * message header, which ties a consumer to the publisher's class names. Here each service reads the payload into
 * its own record, and only the fields it declares; Jackson 3 ignores unknown fields by default, so a publisher can
 * add a field without breaking its consumers.
 */
public class EventCodec {

	private final JsonMapper jsonMapper;

	public EventCodec(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public String write(EventEnvelope<?> envelope) {
		return jsonMapper.writeValueAsString(envelope);
	}

	/**
	 * Reads a message into an envelope with a typed payload. The type is built at runtime (Java erases generics, so
	 * {@code EventEnvelope<OrderCreated>.class} doesn't exist).
	 *
	 * @throws tools.jackson.core.JacksonException when the text isn't a valid envelope
	 */
	public <T> EventEnvelope<T> read(String json, Class<T> payloadType) {
		JavaType type = jsonMapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
		return jsonMapper.readValue(json, type);
	}
}
