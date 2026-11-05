package com.stockline.inventory.entity;

import com.stockline.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A place that holds stock.
 */
@Entity
@Table(name = "warehouses")
public class Warehouse extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 255)
	private String address;

	protected Warehouse() {
		// for JPA
	}

	public Warehouse(String code, String name, String address) {
		this.code = code;
		this.name = name;
		this.address = address;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
