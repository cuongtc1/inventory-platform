package com.stockline.auth.repository;

import com.stockline.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	/** Emails are stored in lower case (AuthService normalizes them), so a plain equality match is enough. */
	Optional<User> findByEmail(String email);
}
