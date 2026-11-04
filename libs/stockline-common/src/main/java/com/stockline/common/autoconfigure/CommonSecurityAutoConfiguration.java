package com.stockline.common.autoconfigure;

import com.stockline.common.security.JwtCheckProperties;
import com.stockline.common.security.JwtSecurity;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Clock;

/**
 * What every service that checks tokens needs, created automatically when it has the resource-server starter:
 * the {@link JwtDecoder} (from {@code stockline.jwt.*}) and the {@link RoleHierarchy}, which Spring Security picks
 * up by itself for {@code hasRole(...)}. Each service only writes its own URL rules.
 *
 * <p>{@code beforeName}: Boot creates a default user with a generated password unless it finds a JwtDecoder, so
 * ours must exist first.
 */
@AutoConfiguration(
		after = CommonTimeAutoConfiguration.class,
		beforeName = "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration")
@ConditionalOnClass({JwtDecoder.class, RoleHierarchy.class})
@EnableConfigurationProperties(JwtCheckProperties.class)
public class CommonSecurityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	JwtDecoder jwtDecoder(JwtCheckProperties properties, Clock clock) {
		return JwtSecurity.jwtDecoder(properties.secret(), properties.issuer(), clock);
	}

	@Bean
	@ConditionalOnMissingBean
	RoleHierarchy roleHierarchy() {
		return JwtSecurity.roleHierarchy();
	}
}
