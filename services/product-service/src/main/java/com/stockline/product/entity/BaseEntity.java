package com.stockline.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Columns every table has: {@code id}, {@code created_at}, {@code updated_at}, {@code created_by},
 * {@code updated_by}.
 *
 * <p>{@code @MappedSuperclass} is not a table itself; its fields become columns of each subclass's table.
 * {@link AuditingEntityListener} (Spring Data JPA auditing, set up in {@code JpaAuditingConfig}) fills the four
 * audit fields just before INSERT and UPDATE, so services never set them.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@Id
	// AUTO_INCREMENT in MySQL: the database generates the id on INSERT
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@CreatedBy
	@Column(name = "created_by", nullable = false, updatable = false, length = 100)
	private String createdBy;

	@LastModifiedBy
	@Column(name = "updated_by", nullable = false, length = 100)
	private String updatedBy;

	public Long getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}
}
