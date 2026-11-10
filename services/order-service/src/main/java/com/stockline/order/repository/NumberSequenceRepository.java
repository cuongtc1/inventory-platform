package com.stockline.order.repository;

import com.stockline.order.entity.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {

	/**
	 * Reads a counter and locks its row: {@code SELECT ... FOR UPDATE}. Another transaction asking for the same
	 * counter waits until this one commits or rolls back, so two orders can't take the same number. Needs a
	 * transaction; the lock ends with it.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<NumberSequence> findByName(String name);
}
