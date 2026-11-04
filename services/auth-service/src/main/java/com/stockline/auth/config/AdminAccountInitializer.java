package com.stockline.auth.config;

import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.entity.User;
import com.stockline.auth.repository.UserRepository;
import com.stockline.auth.service.AuthService;
import com.stockline.common.constant.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first ADMIN when the users table is empty, so a fresh installation can log in at all.
 * The account comes from {@link AdminProperties} (env {@code ADMIN_EMAIL}, {@code ADMIN_PASSWORD}), so no
 * password or hash is committed in a migration. Once any user exists it does nothing: changing ADMIN_PASSWORD
 * later doesn't change an existing account.
 *
 * <p>An {@link ApplicationRunner} runs once, after the application has started (Flyway has created the table).
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminProperties adminProperties;
	private final MessageSource messageSource;

	public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
								   AdminProperties adminProperties, MessageSource messageSource) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminProperties = adminProperties;
		this.messageSource = messageSource;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			return;
		}
		String email = AuthService.normalizeEmail(adminProperties.email());
		userRepository.save(new User(email, passwordEncoder.encode(adminProperties.password()),
				adminProperties.fullName().strip(), UserType.ADMIN, UserStatus.ACTIVE));
		// Never log the password
		log.info(messageSource.getMessage(MessageKeys.USER_ADMIN_CREATED, new Object[] {email}, Locale.ROOT));
	}
}
