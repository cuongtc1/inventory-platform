package com.stockline.auth.repository;

import com.stockline.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Emails are stored in lower case (the services normalize them), so plain equality is enough; MySQL's default
 * collation would also ignore case, like the unique key uk_users_email.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	/** For updates: is the email used by another user than this one? */
	boolean existsByEmailAndIdNot(String email, Long id);
}
