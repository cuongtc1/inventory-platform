package com.stockline.auth.entity;

import com.stockline.auth.constant.UserStatus;
import com.stockline.common.constant.UserType;
import com.stockline.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A person who can log in. The email is the login name; the password is only stored as a BCrypt hash.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

	@Column(nullable = false, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(name = "user_type", nullable = false, length = 20)
	private UserType userType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	protected User() {
		// for JPA
	}

	public User(String email, String passwordHash, String fullName, UserType userType, UserStatus status) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.userType = userType;
		this.status = status;
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}
}
