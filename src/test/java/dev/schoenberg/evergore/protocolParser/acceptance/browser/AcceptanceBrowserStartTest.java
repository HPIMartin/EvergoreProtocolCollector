package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.function.Predicate;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;

@AnalyzeClasses(packages = "dev.schoenberg.evergore.protocolParser.acceptance", importOptions = ImportOption.OnlyIncludeTests.class)
class AcceptanceBrowserStartTest {
	private static final DescribedPredicate<JavaCodeUnit> THE_SHARED_DRIVER_SERVICE = describe("BrowserChoice.geckoDriver",
			unit -> unit.getOwner().isEquivalentTo(BrowserChoice.class) && unit.getName().equals("geckoDriver"));
	private static final String WHY = "the default driver service runs Selenium Manager on every start, and parallel first starts of Selenium 4.7.2 run its binary half-written";

	@ArchTest
	static final ArchRule noAcceptanceBrowserStartsOnTheDefaultDriverService = noClasses()
			.should()
			.callConstructor(FirefoxDriver.class)
			.orShould()
			.callConstructor(FirefoxDriver.class, FirefoxOptions.class)
			.orShould()
			.callMethod(GeckoDriverService.class, "createDefaultService")
			.orShould()
			.callMethod(FirefoxDriver.class, "builder")
			.because(WHY);

	@ArchTest
	static final ArchRule onlyTheSharedDriverServiceBuildsAGeckoDriverService = noCodeUnits()
			.that(DescribedPredicate.not(THE_SHARED_DRIVER_SERVICE))
			.should(call("a GeckoDriverService.Builder constructor",
					call -> call.getTargetOwner().isEquivalentTo(GeckoDriverService.Builder.class) && call.getName().equals("<init>")))
			.because(WHY);

	@ArchTest
	static final ArchRule theSharedDriverServiceHandsOverTheKeptDriver = codeUnits()
			.that(THE_SHARED_DRIVER_SERVICE)
			.should(call("GeckoDriverExecutable.ofThisJvm", call -> call.getTargetOwner().isEquivalentTo(GeckoDriverExecutable.class) && call.getName().equals("ofThisJvm")))
			.andShould(call("GeckoDriverService.Builder.usingDriverExecutable",
					call -> call.getTargetOwner().isEquivalentTo(GeckoDriverService.Builder.class) && call.getName().equals("usingDriverExecutable")))
			.because(WHY);

	private static ArchCondition<JavaCodeUnit> call(String what, Predicate<JavaCall<?>> isTheCall) {
		return new ArchCondition<>("call " + what) {
			@Override
			public void check(JavaCodeUnit unit, ConditionEvents events) {
				boolean calls = unit.getCallsFromSelf().stream().anyMatch(isTheCall);
				events.add(new SimpleConditionEvent(unit, calls, unit.getFullName() + (calls ? " calls " : " does not call ") + what));
			}
		};
	}
}
