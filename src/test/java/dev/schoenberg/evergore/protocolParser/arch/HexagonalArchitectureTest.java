package dev.schoenberg.evergore.protocolParser.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.schoenberg.evergore.protocolParser", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {
	private static final String[] FRAMEWORKS = {"io.micronaut..", "jakarta..", "org.openqa.selenium..", "com.j256.ormlite..", "org.sqlite..", "com.fasterxml.jackson..",
			"io.reactivex..", "org.apache.commons..", "ch.qos.logback..", "org.slf4j..", "io.netty.."};
	private static final String[] ADAPTERS_AND_CONFIG = {"..dataExtraction.website..", "..database..", "..rest..", "..monitoring..", "..helper.config..", "..helper.selenium.."};
	private static final String[] EVERYTHING_OUTSIDE_THE_CORE = {"..application..", "..dataExtraction.website..", "..database..", "..rest..", "..monitoring..", "..helper.config..",
			"..helper.selenium..", "..helper.logger..", "..helper.fileLoader.."};

	@ArchTest
	static final ArchRule domainAndBusinessLogicStayFrameworkFree = noClasses()
			.that()
			.resideInAnyPackage("..domain..", "..businessLogic..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(FRAMEWORKS)
			.because("the domain and businessLogic core must stay framework-free (hexagonal boundary)");

	@ArchTest
	static final ArchRule applicationUseCasesStayFrameworkFree = noClasses()
			.that()
			.resideInAPackage("..application..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(FRAMEWORKS)
			.because("the application use-cases must stay framework-free (hexagonal boundary)");

	@ArchTest
	static final ArchRule applicationDependsOnlyInward = noClasses()
			.that()
			.resideInAPackage("..application..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(ADAPTERS_AND_CONFIG)
			.because("application use-cases must not depend on adapters or config (dependencies point inward)");

	@ArchTest
	static final ArchRule domainAndBusinessLogicDependOnlyInward = noClasses()
			.that()
			.resideInAnyPackage("..domain..", "..businessLogic..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage(EVERYTHING_OUTSIDE_THE_CORE)
			.because("the core must not depend on adapters, config or the use-cases around it (dependencies point inward)");
}
