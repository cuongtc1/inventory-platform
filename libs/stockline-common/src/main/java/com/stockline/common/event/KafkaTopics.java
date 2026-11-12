package com.stockline.common.event;

import java.util.List;

/**
 * The Kafka topics of Stockline (TASKS.md, docs/conventions.md section 6). Every service that uses Kafka creates the
 * missing ones at startup ({@code CommonKafkaAutoConfiguration}), so it doesn't matter which service starts first.
 *
 * <p>The names are the contract between services, which is why they live here; what each event carries (its
 * payload) is decided by the services themselves.
 */
public final class KafkaTopics {

	/** order-service: an order was created and needs stock. */
	public static final String ORDER_CREATED = "order.created";
	/** inventory-service: stock for every line of an order was set aside. */
	public static final String STOCK_RESERVED = "stock.reserved";
	/** inventory-service: an order couldn't get its stock. */
	public static final String STOCK_FAILED = "stock.failed";
	/** order-service: an order was cancelled; its stock can be released. */
	public static final String ORDER_CANCELLED = "order.cancelled";
	/** inventory-service: a product's available stock fell to its reorder level. */
	public static final String STOCK_LOW = "stock.low";

	public static final List<String> ALL = List.of(ORDER_CREATED, STOCK_RESERVED, STOCK_FAILED, ORDER_CANCELLED, STOCK_LOW);

	/**
	 * Partitions per topic. A partition is one ordered log; more partitions let more consumers of a group work in
	 * parallel. Messages with the same key (the order id) always go to the same partition, so one order's events
	 * stay in order.
	 */
	public static final int PARTITIONS = 3;

	/** Copies of each partition. 1, because the local Kafka is a single broker; a real cluster would use 3. */
	public static final short REPLICAS = 1;

	private KafkaTopics() {
	}
}
