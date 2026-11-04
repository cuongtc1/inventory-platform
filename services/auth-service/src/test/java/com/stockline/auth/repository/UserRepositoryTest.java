package com.stockline.auth.repository;

import com.stockline.auth.TestcontainersConfiguration;
import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.dto.UserFilter;
import com.stockline.auth.entity.User;
import com.stockline.common.constant.AuditConstants;
import com.stockline.common.constant.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static com.stockline.auth.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the {@code users} table from V1 against the entity, the lookup by email and the unique email, on MySQL.
 * (Hibernate's {@code ddl-auto: validate} already fails at startup if a column is missing or has the wrong type.)
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void setUp() {
		entityManager.persist(user("linh@corp.com", "$2a$10$hash", UserType.MANAGER, UserStatus.ACTIVE));
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void findByEmail_existingEmail_returnsUserWithAllColumns() {
		// when
		User found = userRepository.findByEmail("linh@corp.com").orElseThrow();

		// then
		assertThat(found.getId()).isNotNull();
		assertThat(found.getPasswordHash()).isEqualTo("$2a$10$hash");
		assertThat(found.getFullName()).isEqualTo("Test manager");
		assertThat(found.getUserType()).isEqualTo(UserType.MANAGER);
		assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getCreatedBy()).isEqualTo(AuditConstants.SYSTEM_USER);
	}

	@Test
	void findByEmail_unknownEmail_returnsEmpty() {
		assertThat(userRepository.findByEmail("nobody@corp.com")).isEmpty();
	}

	@Test
	void existsByEmailAndIdNot_ownEmail_isFalse_otherUsersEmail_isTrue() {
		// given
		User linh = userRepository.findByEmail("linh@corp.com").orElseThrow();
		User tom = userRepository.save(user("tom@corp.com", "$2a$10$hash", UserType.STAFF, UserStatus.ACTIVE));

		// then
		assertThat(userRepository.existsByEmailAndIdNot("linh@corp.com", linh.getId())).isFalse();
		assertThat(userRepository.existsByEmailAndIdNot("linh@corp.com", tom.getId())).isTrue();
	}

	@Test
	void findAll_withFilters_combinesThemWithAnd() {
		// given
		userRepository.save(new User("tom@corp.com", "$2a$10$hash", "Tom Lee", UserType.STAFF, UserStatus.ACTIVE));
		userRepository.save(new User("anna_b@corp.com", "$2a$10$hash", "Anna Linhart", UserType.STAFF, UserStatus.INACTIVE));
		PageRequest byEmail = PageRequest.of(0, 20, Sort.by("email"));

		// when / then: search in email or name, ignoring case
		assertThat(userRepository.findAll(matching("LINH", null, null), byEmail).getContent())
				.extracting(User::getEmail).containsExactly("anna_b@corp.com", "linh@corp.com");
		assertThat(userRepository.findAll(matching("linh", UserType.STAFF, null), byEmail).getContent())
				.extracting(User::getEmail).containsExactly("anna_b@corp.com");
		assertThat(userRepository.findAll(matching(null, null, UserStatus.ACTIVE), byEmail).getContent())
				.extracting(User::getEmail).containsExactly("linh@corp.com", "tom@corp.com");
		// "_" is plain text, not "any one character"
		assertThat(userRepository.findAll(matching("a_b", null, null), byEmail).getContent())
				.extracting(User::getEmail).containsExactly("anna_b@corp.com");
	}

	@Test
	void save_sameEmailTwice_isRejectedByUniqueKey() {
		// when / then
		assertThatThrownBy(() -> userRepository.saveAndFlush(
				user("linh@corp.com", "$2a$10$other", UserType.STAFF, UserStatus.ACTIVE)))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_users_email");
	}

	private static Specification<User> matching(String search, UserType userType, UserStatus status) {
		return UserSpecifications.matching(new UserFilter(search, userType, status));
	}
}
