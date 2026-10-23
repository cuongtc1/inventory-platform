package com.stockline.product.entity;

import com.stockline.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A company we buy products from (table {@code suppliers}).
 */
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

	@Column(nullable = false, length = 150)
	private String name;

	@Column(name = "contact_name", length = 100)
	private String contactName;

	@Column(length = 150)
	private String email;

	@Column(length = 30)
	private String phone;

	protected Supplier() {
		// for JPA
	}

	public Supplier(String name, String contactName, String email, String phone) {
		this.name = name;
		this.contactName = contactName;
		this.email = email;
		this.phone = phone;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
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
