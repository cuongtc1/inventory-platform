package com.stockline.inventory.repository;

import com.stockline.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

	/** The reservations of one order, one per line. */
	List<StockReservation> findByOrderId(Long orderId);
}
