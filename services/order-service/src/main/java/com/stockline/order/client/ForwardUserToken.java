package com.stockline.order.client;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;

/**
 * Adds the token of the user who made the current request to a call to another service, because every service
 * checks tokens (docs/conventions.md section 11). One interceptor for both clients, instead of a header in each call.
 *
 * <p>A {@link ClientHttpRequestInterceptor} runs around every request of the RestClient it is added to, like a servlet
 * filter on the way out.
 */
public class ForwardUserToken implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		request.getHeaders().setBearerAuth(currentToken());
		return execution.execute(request, body);
	}

	private static String currentToken() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			return token.getToken().getTokenValue();
		}
		throw new IllegalStateException("A call to another service needs a signed-in user");
	}
}
