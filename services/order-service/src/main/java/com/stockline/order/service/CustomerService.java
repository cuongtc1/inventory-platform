package com.stockline.order.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.common.util.TextUtils;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.dto.CustomerRequest;
import com.stockline.order.dto.CustomerResponse;
import com.stockline.order.entity.Customer;
import com.stockline.order.mapper.CustomerMapper;
import com.stockline.order.repository.CustomerRepository;
import com.stockline.order.repository.CustomerRepository.CustomerOrderCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Customers. The rules: a name is unique ignoring case, and a customer with orders can't be deleted.
 */
@Service
@Transactional(readOnly = true)
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final CustomerMapper customerMapper;

	public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
		this.customerRepository = customerRepository;
		this.customerMapper = customerMapper;
	}

	public PageResponse<CustomerResponse> list(String search, Pageable pageable) {
		String text = search == null ? "" : search.strip();
		Page<Customer> page = customerRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
				text, text, pageable);
		Map<Long, Long> orderCounts = orderCounts(page.getContent());
		return PageResponse.from(page.map(customer ->
				customerMapper.toResponse(customer, orderCounts.getOrDefault(customer.getId(), 0L))));
	}

	public CustomerResponse get(Long id) {
		Customer customer = findCustomer(id);
		return customerMapper.toResponse(customer, orderCount(customer));
	}

	@Transactional
	public CustomerResponse create(CustomerRequest request) {
		String name = request.name().strip();
		if (customerRepository.existsByName(name)) {
			throw duplicateName(name);
		}

		Customer saved = customerRepository.save(new Customer(name, TextUtils.blankToNull(request.email()),
				TextUtils.blankToNull(request.phone())));
		return customerMapper.toResponse(saved, 0);
	}

	@Transactional
	public CustomerResponse update(Long id, CustomerRequest request) {
		Customer customer = findCustomer(id);
		String name = request.name().strip();
		if (customerRepository.existsByNameAndIdNot(name, id)) {
			throw duplicateName(name);
		}

		customer.setName(name);
		customer.setEmail(TextUtils.blankToNull(request.email()));
		customer.setPhone(TextUtils.blankToNull(request.phone()));
		// Run the UPDATE now so JPA auditing sets updatedAt / updatedBy before we build the response
		customerRepository.flush();
		return customerMapper.toResponse(customer, orderCount(customer));
	}

	/** Hard delete, the project rule. If an order slips in after this check, its foreign key gives the common 409 IN_USE. */
	@Transactional
	public void delete(Long id) {
		Customer customer = findCustomer(id);
		if (orderCount(customer) > 0) {
			throw new ResourceInUseException(MessageKeys.CUSTOMER_DELETE_HAS_ORDERS, customer.getName());
		}
		customerRepository.delete(customer);
	}

	private long orderCount(Customer customer) {
		return orderCounts(List.of(customer)).getOrDefault(customer.getId(), 0L);
	}

	private Map<Long, Long> orderCounts(List<Customer> customers) {
		if (customers.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = customers.stream().map(Customer::getId).toList();
		return customerRepository.countOrdersByCustomerIds(ids).stream()
				.collect(Collectors.toMap(CustomerOrderCount::getCustomerId, CustomerOrderCount::getOrderCount));
	}

	private Customer findCustomer(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Customer.class.getSimpleName(), id));
	}

	private static DuplicateResourceException duplicateName(String name) {
		return new DuplicateResourceException(MessageKeys.CUSTOMER_NAME_DUPLICATE, name);
	}
}
