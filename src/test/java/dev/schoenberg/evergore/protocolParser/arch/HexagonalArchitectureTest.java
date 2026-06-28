package dev.schoenberg.evergore.protocolParser.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.schoenberg.evergore.protocolParser", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domainAndBusinessLogicStayFrameworkFree = noClasses()
			.that()
			.resideInAnyPackage("..domain..", "..businessLogic..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("io.micronaut..", "jakarta..", "org.openqa.selenium..", "com.j256.ormlite..", "org.sqlite..", "com.fasterxml.jackson..", "io.reactivex..",
					"org.apache.commons..", "ch.qos.logback..", "org.slf4j..", "io.netty..")
			.because("the domain and businessLogic core must stay framework-free (hexagonal boundary)");

	@ArchTest
	static final ArchRule applicationUseCasesStayFrameworkFree = noClasses()
			.that()
			.resideInAPackage("..application..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("io.micronaut..", "jakarta..", "org.openqa.selenium..", "com.j256.ormlite..", "org.sqlite..", "com.fasterxml.jackson..", "io.reactivex..",
					"org.apache.commons..", "ch.qos.logback..", "org.slf4j..", "io.netty..")
			.because("the application use-cases must stay framework-free (hexagonal boundary)");

	@ArchTest
	static final ArchRule applicationDependsOnlyInward = noClasses()
			.that()
			.resideInAPackage("..application..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("..dataExtraction.website..", "..database..", "..rest..", "..monitoring..", "..helper.config..", "..helper.selenium..")
			.because("application use-cases must not depend on adapters or config (dependencies point inward)");
}
