package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.security.Principal;
import java.time.Instant;

import io.micronaut.core.bind.ArgumentBinder.BindingResult;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.simple.SimpleHttpRequestFactory;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPrincipalBinderTest {
	private static final Instant MADE = Instant.parse("2026-01-05T10:00:00Z");
	private static final ArgumentConversionContext<Principal> PRINCIPAL = ConversionContext.of(Argument.of(Principal.class));

	private final AdminPrincipalBinder tested = new AdminPrincipalBinder();

	@Test
	void bindsTheAccountOfAnAdminLoginAsThePrincipal() {
		MutableHttpRequest<?> request = requestWithLoginOf(Role.ADMIN);

		BindingResult<Principal> bound = tested.bind(PRINCIPAL, request);

		assertThat(bound.getValue()).map(Principal::getName).contains("Calix");
	}

	@Test
	void bindsNoPrincipalForAMembersLogin() {
		MutableHttpRequest<?> request = requestWithLoginOf(Role.MEMBER);

		BindingResult<Principal> bound = tested.bind(PRINCIPAL, request);

		assertThat(bound.getValue()).isEmpty();
	}

	@Test
	void bindsNoPrincipalForARequestWithoutALogin() {
		MutableHttpRequest<?> request = new SimpleHttpRequestFactory().get("/health");

		BindingResult<Principal> bound = tested.bind(PRINCIPAL, request);

		assertThat(bound.getValue()).isEmpty();
	}

	private static MutableHttpRequest<?> requestWithLoginOf(Role role) {
		MutableHttpRequest<?> request = new SimpleHttpRequestFactory().get("/health");
		request.setAttribute(LoginAttribute.NAME, new Login("Calix", role, 0, LoginSource.ACCOUNT, "id", MADE, MADE.plus(Login.LIFETIME)));
		return request;
	}
}
