package dev.schoenberg.evergore.protocolParser.arch;

import com.tngtech.archunit.core.importer.*;
import com.tngtech.archunit.junit.*;
import com.tngtech.archunit.lang.*;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@AnalyzeClasses(packages = "dev.schoenberg.evergore.protocolParser", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domainAndBusinessLogicStayFrameworkFree = noClasses().that().resideInAnyPackage("..domain..", "..businessLogic..").should().dependOnClassesThat()
			.resideInAnyPackage("io.micronaut..", "jakarta..", "org.openqa.selenium..", "com.j256.ormlite..", "org.sqlite..", "com.fasterxml.jackson..", "io.reactivex..",
					"org.apache.commons..", "ch.qos.logback..", "org.slf4j..", "io.netty..")
			.because("the domain and businessLogic core must stay framework-free (hexagonal boundary)");
}
