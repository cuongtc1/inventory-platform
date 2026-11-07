package com.stockline.order.entity;

import com.stockline.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A company we sell to.
 */
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 150)
	private String email;

	@Column(length = 30)
	private String phone;

	protected Customer() {
		// for JPA
	}

	public Customer(String name, String email, String phone) {
		this.name = name;
		this.email = email;
		this.phone = phone;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
}
