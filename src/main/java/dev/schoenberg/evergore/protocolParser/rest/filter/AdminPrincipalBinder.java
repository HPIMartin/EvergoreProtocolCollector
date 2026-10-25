package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.security.Principal;
import java.util.Optional;

import jakarta.inject.Singleton;

import io.micronaut.core.bind.ArgumentBinder.BindingResult;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

@Singleton
public class AdminPrincipalBinder implements TypedRequestArgumentBinder<Principal> {
	@Override
	public Argument<Principal> argumentType() {
		return Argument.of(Principal.class);
	}

	@Override
	public BindingResult<Principal> bind(ArgumentConversionContext<Principal> context, HttpRequest<?> source) {
		Optional<Principal> admin = source.getAttribute(LoginAttribute.NAME, Login.class).filter(login -> login.role() == Role.ADMIN).map(AdminPrincipalBinder::principalOf);
		return () -> admin;
	}

	private static Principal principalOf(Login login) {
		return login::accountName;
	}
}
