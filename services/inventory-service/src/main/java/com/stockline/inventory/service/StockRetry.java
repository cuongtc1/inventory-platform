package com.stockline.inventory.service;

/**
 * How often and how fast a change to stock levels is tried again after a conflict, for every {@code @Retryable} on
 * stock (StockReserver, StockAdjuster). Annotation attributes must be constants, hence this class.
 *
 * <p>A conflict means another transaction changed the same stock level first: the {@code @Version} check refused our
 * UPDATE (optimistic locking), or MySQL picked our transaction to undo a deadlock. Both are
 * {@code ConcurrencyFailureException}s. Trying again re-reads the stock, so the rules are checked against the new
 * numbers: a retry never takes stock that the winner already took.
 *
 * <p>The waits grow (20, 40, 80 ms, ... at most 500 ms), with up to 10 ms of random extra ("jitter"), so transactions
 * that collided don't collide again at exactly the same moment.
 */
public final class StockRetry {

	/** Retries after the first attempt, so up to 6 attempts in all. */
	public static final long MAX_RETRIES = 5;
	public static final long DELAY_MS = 20;
	public static final long JITTER_MS = 10;
	public static final double MULTIPLIER = 2;
	public static final long MAX_DELAY_MS = 500;

	private StockRetry() {
	}
}
