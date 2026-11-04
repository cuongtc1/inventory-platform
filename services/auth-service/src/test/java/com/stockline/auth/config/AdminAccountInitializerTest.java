package com.stockline.auth.config;

import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.entity.User;
import com.stockline.auth.repository.UserRepository;
import com.stockline.common.constant.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

	@Mock
	private UserRepository userRepository;

	private AdminAccountInitializer initializer;

	@BeforeEach
	void setUp() {
		StaticMessageSource messages = new StaticMessageSource();
		messages.setUseCodeAsDefaultMessage(true);
		initializer = new AdminAccountInitializer(userRepository, PASSWORD_ENCODER,
				new AdminProperties(" Admin@Stockline.LOCAL ", "admin-pass-1", " Stockline Admin "), messages);
	}

	@Test
	void run_noUsers_createsActiveAdminWithHashedPassword() {
		// given
		when(userRepository.count()).thenReturn(0L);

		// when
		initializer.run(null);

		// then
		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		User admin = saved.getValue();
		assertThat(admin.getEmail()).isEqualTo("admin@stockline.local");
		assertThat(admin.getFullName()).isEqualTo("Stockline Admin");
		assertThat(admin.getUserType()).isEqualTo(UserType.ADMIN);
		assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(admin.getPasswordHash()).isNotEqualTo("admin-pass-1").startsWith("$2a$");
		assertThat(PASSWORD_ENCODER.matches("admin-pass-1", admin.getPasswordHash())).isTrue();
	}

	@Test
	void run_usersExist_createsNothing() {
		// given
		when(userRepository.count()).thenReturn(3L);

		// when
		initializer.run(null);

		// then
		verify(userRepository, never()).save(any());
	}
}
