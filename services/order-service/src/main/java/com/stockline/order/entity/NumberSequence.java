package com.stockline.order.entity;

import com.stockline.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A counter for document numbers (see V5__create_number_sequences.sql). Read it only through
 * {@code NumberSequenceRepository.findByName}, which locks the row.
 */
@Entity
@Table(name = "number_sequences")
public class NumberSequence extends BaseEntity {

	/** The counter of sales order numbers. */
	public static final String SALES_ORDER = "sales_order";

	@Column(nullable = false, length = 50)
	private String name;

	@Column(name = "next_value", nullable = false)
	private long nextValue;

	protected NumberSequence() {
		// for JPA
	}

	/** Returns the next number and moves the counter on. */
	public long take() {
		return nextValue++;
	}

	public String getName() {
		return name;
	}

	public long getNextValue() {
		return nextValue;
	}
}
