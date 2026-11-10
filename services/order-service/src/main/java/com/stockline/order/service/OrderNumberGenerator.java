package com.stockline.order.service;

import com.stockline.order.entity.NumberSequence;
import com.stockline.order.repository.NumberSequenceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hands out order numbers: SO-1001, SO-1002, ... The counter row stays locked until the caller's transaction ends,
 * so a second order waits for the first instead of getting the same number.
 *
 * <p>MANDATORY: called without a transaction it fails at once. Its own short transaction would release the lock
 * before the order is saved, and the number could then be lost or, worse, be fine only by luck.
 */
@Component
public class OrderNumberGenerator {

	private static final String SALES_ORDER_FORMAT = "SO-%04d";

	private final NumberSequenceRepository numberSequenceRepository;

	public OrderNumberGenerator(NumberSequenceRepository numberSequenceRepository) {
		this.numberSequenceRepository = numberSequenceRepository;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public String nextSalesOrderNumber() {
		NumberSequence sequence = numberSequenceRepository.findByName(NumberSequence.SALES_ORDER)
				.orElseThrow(() -> new IllegalStateException("Missing row in number_sequences: " + NumberSequence.SALES_ORDER));
		// Changed inside the transaction: Hibernate writes the new value when it commits (dirty checking)
		return SALES_ORDER_FORMAT.formatted(sequence.take());
	}
}
