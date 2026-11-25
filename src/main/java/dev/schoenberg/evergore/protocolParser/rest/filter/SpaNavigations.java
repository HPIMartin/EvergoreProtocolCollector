package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;

@Singleton
public class SpaNavigations {
	private final PathCanonicalizer canonicalizer;
	private final SpaNavigationPaths paths;

	public SpaNavigations(PathCanonicalizer canonicalizer, SpaNavigationPaths paths) {
		this.canonicalizer = canonicalizer;
		this.paths = paths;
	}

	public boolean isOne(HttpRequest<?> request) {
		return request.getMethod() == HttpMethod.GET && request.accept().contains(MediaType.TEXT_HTML_TYPE) && paths.isSpaOwned(canonicalizer.canonicalize(request.getPath()));
	}
}
