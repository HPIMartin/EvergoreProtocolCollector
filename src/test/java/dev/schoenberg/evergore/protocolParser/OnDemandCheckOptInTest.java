package dev.schoenberg.evergore.protocolParser;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

class OnDemandCheckOptInTest {
	private static final int ON_DEMAND_CHECKS_IN_THE_SUITE = 3;

	@Test
	void everyOnDemandCheckIsSwitchedByAPropertyTheBuildForwards() {
		assertThat(onDemandChecks())
				.allSatisfy(check -> assertThat(System.getProperty(optInPropertyOf(check)))
						.as("the build must forward %s, or %s can never be switched on", optInPropertyOf(check), check.getSimpleName())
						.isNotNull());
	}

	@Test
	void keepsEveryOnDemandCheckOffUntilItsOwnPropertyIsTrue() {
		assertThat(onDemandChecks())
				.allSatisfy(check -> assertThat(check.getAnnotationOfType(EnabledIfSystemProperty.class).matches())
						.as("%s must be switched on by its property being true", check.getSimpleName())
						.isEqualTo("true"));
	}

	@Test
	void leavesEveryOptInOffInAnOrdinaryRun() {
		assertThat(onDemandChecks()).allSatisfy(check -> assertThat(System.getProperty(optInPropertyOf(check))).as(check.getSimpleName()).isNotEqualTo("true"));
	}

	@Test
	void findsEveryOnDemandCheckTheSuiteHolds() {
		assertThat(onDemandChecks()).as("a check that escapes this discovery is pinned by nothing at all").hasSize(ON_DEMAND_CHECKS_IN_THE_SUITE);
	}

	private static List<JavaClass> onDemandChecks() {
		JavaClasses everything = new ClassFileImporter().importPackages("dev.schoenberg.evergore.protocolParser");
		return everything.stream().filter(type -> type.isAnnotatedWith(EnabledIfSystemProperty.class)).toList();
	}

	private static String optInPropertyOf(JavaClass check) {
		return check.getAnnotationOfType(EnabledIfSystemProperty.class).named();
	}
}
