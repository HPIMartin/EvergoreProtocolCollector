package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.net.URLEncoder;
import java.util.Optional;

import jakarta.inject.Singleton;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.exceptions.RoleMissing;
import dev.schoenberg.evergore.protocolParser.rest.controller.LoginPageController;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
@Filter("/**")
public class AccessFilter implements HttpServerFilter {
	private static final String LOCATION = "Location";
	private static final String NEXT = "next";
	private static final String ADMIN_PATH = "/api/v1/admin";

	private final PathCanonicalizer canonicalizer;
	private final PublicPaths publicPaths;
	private final SpaNavigationPaths spaNavigationPaths;
	private final Logger logger;

	public AccessFilter(PathCanonicalizer canonicalizer, PublicPaths publicPaths, SpaNavigationPaths spaNavigationPaths, Logger logger) {
		this.canonicalizer = canonicalizer;
		this.publicPaths = publicPaths;
		this.spaNavigationPaths = spaNavigationPaths;
		this.logger = logger;
	}

	@Override
	public int getOrder() {
		return FilterOrder.ACCESS.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		String path = canonicalizer.canonicalize(request.getPath());
		if (publicPaths.contains(path)) {
			return chain.proceed(request);
		}
		Optional<Login> login = request.getAttribute(LoginAttribute.NAME, Login.class);
		if (login.isPresent()) {
			if (isTheAdminsPath(path) && login.get().role() != Role.ADMIN) {
				throw new RoleMissing();
			}
			return chain.proceed(request);
		}

		logger.info("Access rejected");
		if (isANavigation(request, path)) {
			return Publishers.just(HttpResponse.status(HttpStatus.FOUND).header(LOCATION, loginAddressFor(path, request)));
		}
		throw new AccessNotAllowed();
	}

	private boolean isANavigation(HttpRequest<?> request, String canonicalPath) {
		return request.getMethod() == HttpMethod.GET && spaNavigationPaths.isSpaOwned(canonicalPath) && request.accept().contains(MediaType.TEXT_HTML_TYPE);
	}

	private static String loginAddressFor(String canonicalPath, HttpRequest<?> request) {
		String rawQuery = request.getUri().getRawQuery();
		String wayBack = rawQuery == null ? canonicalPath : canonicalPath + "?" + rawQuery;

		return LoginPageController.PATH + "?" + NEXT + "=" + URLEncoder.encode(wayBack, UTF_8);
	}

	private static boolean isTheAdminsPath(String canonicalPath) {
		return canonicalPath.equals(ADMIN_PATH) || canonicalPath.startsWith(ADMIN_PATH + "/");
	}
}
