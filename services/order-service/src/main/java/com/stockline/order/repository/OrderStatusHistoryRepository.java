package com.stockline.order.repository;

import com.stockline.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

	/** The timeline of one order, oldest first. */
	List<OrderStatusHistory> findBySalesOrderIdOrderByCreatedAtAscIdAsc(Long salesOrderId);
}
