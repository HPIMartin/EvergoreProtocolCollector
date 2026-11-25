package dev.schoenberg.evergore.protocolParser.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.schoenberg.evergore.protocolParser", importOptions = ImportOption.DoNotIncludeTests.class)
class UsageRecordsHoldNoAddressTest {
	@ArchTest
	static final ArchRule nothingThatRecordsTheUsageReadsTheClientsAddress = noClasses()
			.that()
			.resideInAPackage("..usage..")
			.or()
			.haveSimpleNameContaining("Usage")
			.or()
			.haveSimpleNameContaining("PageLoad")
			.should()
			.dependOnClassesThat()
			.haveSimpleName("ClientIp");
}
