package com.stockline.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

/**
 * Columns every table has: {@code id}, {@code created_at}, {@code updated_at}.
 *
 * <p>{@code @MappedSuperclass} is not a table itself; its fields become columns of each subclass's table.
 * The timestamps are set by JPA lifecycle callbacks just before INSERT and UPDATE, so services never set them.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    // AUTO_INCREMENT in MySQL: the database generates the id on INSERT
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
