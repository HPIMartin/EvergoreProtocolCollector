package dev.schoenberg.evergore.protocolParser.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.schoenberg.evergore.protocolParser", importOptions = ImportOption.DoNotIncludeTests.class)
class NoApplicationTextInHttpStatusTest {

	@ArchTest
	static final ArchRule noCodeSetsAReasonPhraseOnAnHttpStatus = noClasses()
			.should()
			.callMethod(HttpResponse.class, "status", HttpStatus.class, String.class)
			.orShould()
			.callMethod(HttpResponse.class, "status", int.class, String.class)
			.orShould()
			.callMethod(MutableHttpResponse.class, "status", HttpStatus.class, CharSequence.class)
			.orShould()
			.callMethod(MutableHttpResponse.class, "status", int.class, CharSequence.class)
			.orShould()
			.callMethod(HttpResponseFactory.class, "status", HttpStatus.class, String.class)
			.orShould()
			.callMethod(HttpResponseFactory.class, "status", int.class, String.class)
			.because(
					"a reason phrase can only ever carry application text, and a control character in it makes Netty refuse the response, turning a client error into a caller-triggerable 500");
}
