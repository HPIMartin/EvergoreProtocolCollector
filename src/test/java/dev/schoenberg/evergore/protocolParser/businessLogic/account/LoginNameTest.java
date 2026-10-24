package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LoginNameTest {
	private static final String COMBINING_DIAERESIS = Character.toString(0x0308);
	private static final String NO_BREAK_SPACE = Character.toString(0x00A0);

	private final Locale defaultLocale = Locale.getDefault();

	@AfterEach
	void restoreTheDefaultLocale() {
		Locale.setDefault(defaultLocale);
	}

	@ParameterizedTest
	@CsvSource({"aurora,Aurora", "AURORA,Aurora", "Aurora,Aurora", "aUrOrA,Aurora", "ärger,Ärger", "ÄRGER,Ärger", "Ärger,Ärger"})
	void matchesAnAvatarNameRegardlessOfCapitals(String typed, String avatar) {
		LoginName tested = new LoginName(typed);

		boolean matches = tested.matches(avatar);

		assertThat(matches).isTrue();
	}

	@ParameterizedTest
	@CsvSource({"Aurora,Aurore", "Aurora,Aurora2", "Aurora,Aur ora", "Ärger,Arger", "WEISS,Weiß", "Weiß,WEISS"})
	void matchesNoOtherAvatarName(String typed, String avatar) {
		LoginName tested = new LoginName(typed);

		boolean matches = tested.matches(avatar);

		assertThat(matches).isFalse();
	}

	@ParameterizedTest
	@CsvSource({"aurora,AURORA", "Aurora,aUrOrA", "ärger,ÄRGER"})
	void foldsNamesThatDifferInCapitalsToOneKey(String first, String second) {
		String firstKey = new LoginName(first).key();

		String secondKey = new LoginName(second).key();

		assertThat(firstKey).isEqualTo(secondKey);
	}

	@Test
	void matchesADecomposedNameWithTheComposedOneAndTheOtherWayAround() {
		String decomposed = "A" + COMBINING_DIAERESIS + "rger";
		LoginName typedDecomposed = new LoginName(decomposed);
		LoginName typedComposed = new LoginName("Ärger");

		boolean decomposedMatchesComposed = typedDecomposed.matches("Ärger");
		boolean composedMatchesDecomposed = typedComposed.matches(decomposed);

		assertThat(decomposedMatchesComposed).isTrue();
		assertThat(composedMatchesDecomposed).isTrue();
	}

	@ParameterizedTest
	@CsvSource({"'Aurora ',Aurora", "' Aurora',Aurora", "Aurora,'Aurora '", "'  AURORA  ',Aurora"})
	void matchesAnAvatarNameDespiteSurroundingBlanks(String typed, String avatar) {
		LoginName tested = new LoginName(typed);

		boolean matches = tested.matches(avatar);

		assertThat(matches).isTrue();
	}

	@Test
	void foldsAnEnclosingNoBreakSpaceAway() {
		LoginName tested = new LoginName(NO_BREAK_SPACE + "Aurora" + NO_BREAK_SPACE);

		String key = tested.key();

		assertThat(key).isEqualTo("aurora");
	}

	@Test
	void keepsTheKeyOfADecomposedNameEqualToTheComposedKey() {
		String decomposedKey = new LoginName("A" + COMBINING_DIAERESIS + "RGER").key();

		String composedKey = new LoginName("Ärger").key();

		assertThat(decomposedKey).isEqualTo(composedKey);
	}

	@Test
	void foldsToTheLowerCaseName() {
		LoginName tested = new LoginName("AURORA");

		String key = tested.key();

		assertThat(key).isEqualTo("aurora");
	}

	@Test
	void stillMatchesAnAvatarNameWithAnIUnderTheTurkishDefaultLocale() {
		Locale.setDefault(Locale.forLanguageTag("tr"));
		LoginName tested = new LoginName("IVO");

		boolean matches = tested.matches("ivo");

		assertThat(matches).isTrue();
	}

	@Test
	void keepsTheKeyOfAnIFreeOfTheTurkishDefaultLocale() {
		Locale.setDefault(Locale.forLanguageTag("tr"));
		LoginName tested = new LoginName("AURORA IVO");

		String key = tested.key();

		assertThat(key).isEqualTo("aurora ivo");
	}
}
