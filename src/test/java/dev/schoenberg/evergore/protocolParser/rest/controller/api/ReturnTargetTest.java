package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnTargetTest {
	@ParameterizedTest
	@ValueSource(strings = {"/overview", "/avatars/Aurora/storage", "/avatars/Aurora/bank?page=2&sort=amount", "/avatars/%C3%84rger/bank", "/explanation", "/"})
	void keepsAPageOfThisService(String next) {
		String target = ReturnTarget.of(next);

		assertThat(target).isEqualTo(next);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"https://evil.example/overview", "http:evil.example", "javascript:alert(1)", "overview", "//evil.example", "/\\evil.example", "\\\\evil.example",
			"/%2Fevil.example", "/%2fevil.example", "/%5Cevil.example", "%2F%2Fevil.example", "/\t/evil.example", "/\n/evil.example", "/%E0%A4%A"})
	void leadsToTheOverviewInsteadOfAnythingElse(String next) {
		String target = ReturnTarget.of(next);

		assertThat(target).isEqualTo("/");
	}
}
