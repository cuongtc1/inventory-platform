package com.stockline.common.event;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventCodecTest {

	/** A payload like a service would declare it. */
	record OrderLines(long orderId, List<Line> items) {
	}

	record Line(long productId, int quantity) {
	}

	/** What a consumer that only cares about the order id declares. */
	record OrderIdOnly(long orderId) {
	}

	private static final UUID ID = UUID.fromString("6f1c2a1e-6a43-4c3e-9b8a-0d6f4b1e2c55");
	private static final Instant AT = Instant.parse("2026-10-22T08:15:30Z");

	private final EventCodec codec = new EventCodec(JsonMapper.builder().build());

	@Test
	void write_producesTheConventionsEnvelope() {
		// when
		String json = codec.write(new EventEnvelope<>(ID, "order.created", AT, new OrderLines(42, List.of(new Line(12, 2)))));

		// then: the shape of docs/conventions.md section 6, with the time as ISO text
		assertThat(json).isEqualTo("""
				{"eventId":"6f1c2a1e-6a43-4c3e-9b8a-0d6f4b1e2c55","eventType":"order.created",\
				"occurredAt":"2026-10-22T08:15:30Z","payload":{"orderId":42,"items":[{"productId":12,"quantity":2}]}}""");
	}

	@Test
	void read_givesATypedPayload() {
		// given
		String json = codec.write(new EventEnvelope<>(ID, "order.created", AT, new OrderLines(42, List.of(new Line(12, 2)))));

		// when
		EventEnvelope<OrderLines> envelope = codec.read(json, OrderLines.class);

		// then
		assertThat(envelope.eventId()).isEqualTo(ID);
		assertThat(envelope.occurredAt()).isEqualTo(AT);
		assertThat(envelope.payload().items()).containsExactly(new Line(12, 2));
	}

	@Test
	void read_ignoresFieldsTheConsumerDoesntDeclare() {
		// A consumer reads only what it needs, so a publisher can add fields without breaking it
		String json = codec.write(new EventEnvelope<>(ID, "order.created", AT, new OrderLines(42, List.of(new Line(12, 2)))));

		assertThat(codec.read(json, OrderIdOnly.class).payload()).isEqualTo(new OrderIdOnly(42));
	}

	@Test
	void read_notAnEnvelope_throws() {
		assertThatThrownBy(() -> codec.read("not json", OrderIdOnly.class)).isInstanceOf(JacksonException.class);
	}
}
