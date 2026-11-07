package com.stockline.order.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.dto.CustomerRequest;
import com.stockline.order.dto.CustomerResponse;
import com.stockline.order.entity.Customer;
import com.stockline.order.mapper.CustomerMapper;
import com.stockline.order.repository.CustomerRepository;
import com.stockline.order.repository.CustomerRepository.CustomerOrderCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.stockline.order.TestData.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: no Spring, no database. The repository is a Mockito mock; the mapper is the real class.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	@Mock
	private CustomerRepository customerRepository;

	private CustomerService customerService;

	@BeforeEach
	void setUp() {
		customerService = new CustomerService(customerRepository, new CustomerMapper());
	}

	@Test
	void list_searchWithSpaces_trimsItAndAddsOrderCounts() {
		// given: Nova has no orders, so it isn't in the count result
		Pageable pageable = PageRequest.of(0, 20);
		Customer acme = customer(1L, "ACME Ltd", "buy@acme.com", null);
		Customer nova = customer(2L, "Nova Retail", null, null);
		when(customerRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase("ltd", "ltd", pageable))
				.thenReturn(new PageImpl<>(List.of(acme, nova), pageable, 2));
		when(customerRepository.countOrdersByCustomerIds(List.of(1L, 2L))).thenReturn(List.of(orderCount(1L, 18)));

		// when
		PageResponse<CustomerResponse> page = customerService.list(" ltd  ", pageable);

		// then
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.content()).extracting(CustomerResponse::orderCount).containsExactly(18L, 0L);
	}

	@Test
	void list_emptyPage_doesNotCount() {
		// given
		Pageable pageable = PageRequest.of(0, 20);
		when(customerRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase("", "", pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		// when
		customerService.list(null, pageable);

		// then: "IN ()" would be invalid SQL
		verify(customerRepository, never()).countOrdersByCustomerIds(any());
	}

	@Test
	void get_unknownId_throwsNotFound() {
		// given
		when(customerRepository.findById(9L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> customerService.get(9L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_trimsTextAndStoresBlankContactsAsNull() {
		// given
		when(customerRepository.existsByName("ACME Ltd")).thenReturn(false);
		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		CustomerResponse created = customerService.create(new CustomerRequest(" ACME Ltd ", "", "  "));

		// then
		ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepository).save(saved.capture());
		assertThat(saved.getValue().getName()).isEqualTo("ACME Ltd");
		assertThat(saved.getValue().getEmail()).isNull();
		assertThat(saved.getValue().getPhone()).isNull();
		assertThat(created.orderCount()).isZero();
	}

	@Test
	void create_nameTaken_throwsDuplicate() {
		// given
		when(customerRepository.existsByName("ACME Ltd")).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> customerService.create(new CustomerRequest("ACME Ltd", null, null)))
				.isInstanceOfSatisfying(DuplicateResourceException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.CUSTOMER_NAME_DUPLICATE);
					assertThat(ex.getMessageArgs()).containsExactly("ACME Ltd");
				});
		verify(customerRepository, never()).save(any());
	}

	@Test
	void update_changesEveryFieldAndFlushes() {
		// given
		Customer acme = customer(1L, "ACME", "old@acme.com", "+84 1");
		when(customerRepository.findById(1L)).thenReturn(Optional.of(acme));
		when(customerRepository.existsByNameAndIdNot("ACME Ltd", 1L)).thenReturn(false);
		when(customerRepository.countOrdersByCustomerIds(List.of(1L))).thenReturn(List.of(orderCount(1L, 5)));

		// when
		CustomerResponse updated = customerService.update(1L, new CustomerRequest("ACME Ltd", "buy@acme.com", null));

		// then
		assertThat(updated.name()).isEqualTo("ACME Ltd");
		assertThat(updated.email()).isEqualTo("buy@acme.com");
		assertThat(updated.phone()).isNull();
		assertThat(updated.orderCount()).isEqualTo(5);
		verify(customerRepository).flush();
	}

	@Test
	void update_nameOfAnotherCustomer_throwsDuplicate() {
		// given
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "ACME", null, null)));
		when(customerRepository.existsByNameAndIdNot("Nova", 1L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> customerService.update(1L, new CustomerRequest("Nova", null, null)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void delete_withoutOrders_deletes() {
		// given
		Customer acme = customer(1L, "ACME", null, null);
		when(customerRepository.findById(1L)).thenReturn(Optional.of(acme));
		when(customerRepository.countOrdersByCustomerIds(List.of(1L))).thenReturn(List.of());

		// when
		customerService.delete(1L);

		// then
		verify(customerRepository).delete(acme);
	}

	@Test
	void delete_withOrders_throwsInUse() {
		// given
		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "ACME", null, null)));
		when(customerRepository.countOrdersByCustomerIds(List.of(1L))).thenReturn(List.of(orderCount(1L, 3)));

		// when / then
		assertThatThrownBy(() -> customerService.delete(1L))
				.isInstanceOfSatisfying(ResourceInUseException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.CUSTOMER_DELETE_HAS_ORDERS);
					assertThat(ex.getMessageArgs()).containsExactly("ACME");
				});
		verify(customerRepository, never()).delete(any(Customer.class));
	}

	private static CustomerOrderCount orderCount(long customerId, long count) {
		return new CustomerOrderCount() {
			@Override
			public long getCustomerId() {
				return customerId;
			}

			@Override
			public long getOrderCount() {
				return count;
			}
		};
	}
}
