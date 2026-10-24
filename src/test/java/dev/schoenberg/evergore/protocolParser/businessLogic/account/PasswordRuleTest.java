package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PasswordRuleTest {
	private static final String BASE = "abcdefg1";
	private static final String A_UMLAUT = "Ä";
	private static final String COMBINING_DIAERESIS = text(0x0308);

	private final PasswordRule tested = new PasswordRule();

	private static String text(int... codePoints) {
		return new String(codePoints, 0, codePoints.length);
	}

	private static String loneSurrogate() {
		return String.valueOf((char) 0xD800);
	}

	@ParameterizedTest
	@ValueSource(strings = {"abcdefg1!", "12345678!", "!!!!!!!1", "Gilde-2026", "Ärger-Ärger9", "Pass wort1!", "a1+bcdef"})
	void acceptsAPasswordWithEightCharactersADigitAndASpecialCharacter(String password) {
		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(password);

		assertThat(refusal).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "a1!", "abc1!de", "abcdefgh!", "abcdefgh1", "abcdefgh", "ärgerlich1", "Pass wort1", "1234 5678", "\t\t\t\t\t\t1\t1"})
	void refusesAPasswordThatBreaksTheRule(String password) {
		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(password);

		assertThat(refusal).contains(PasswordRefusal.BREAKS_THE_RULE);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ä", "Ä", "ß", "é", "Z"})
	void countsALetterAsNoSpecialCharacter(String letter) {
		String chosen = BASE + letter;

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(chosen);

		assertThat(refusal).contains(PasswordRefusal.BREAKS_THE_RULE);
	}

	@ParameterizedTest
	@ValueSource(strings = {"!", "-", "_", "§", "€", "@", "\"", "'"})
	void countsAnyOtherVisibleCharacterAsSpecial(String special) {
		String chosen = BASE + special;

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(chosen);

		assertThat(refusal).isEmpty();
	}

	static IntStream notSpecialCodePoints() {
		return IntStream.of(0x0020, 0x0009, 0x00A0, 0x2007, 0x202F, 0x2028, 0xFEFF, 0x001C, 0x001F, 0x0085, 0x0308, 0x200B);
	}

	@ParameterizedTest
	@MethodSource("notSpecialCodePoints")
	void countsNoSpaceControlOrCombiningCodePointAsSpecial(int codePoint) {
		String chosen = BASE + text(codePoint);

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(chosen);

		assertThat(refusal).contains(PasswordRefusal.BREAKS_THE_RULE);
	}

	@Test
	void countsALoneSurrogateAsNoSpecialCharacter() {
		String chosen = BASE + loneSurrogate();

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(chosen);

		assertThat(refusal).contains(PasswordRefusal.BREAKS_THE_RULE);
	}

	static IntStream specialCodePoints() {
		return IntStream.of('+', '^', '`', '~', '$', '|', '<', 0x00A9, 0x20AC, 0x1F600, 0x00BF, 0x2014, 0x201E);
	}

	@ParameterizedTest
	@MethodSource("specialCodePoints")
	void countsAPunctuationOrSymbolCodePointAsSpecial(int codePoint) {
		String chosen = BASE + text(codePoint);

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(chosen);

		assertThat(refusal).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"bc-123", "bcdefg1"})
	void judgesADecomposedLetterAsTheOneLetterItShows(String rest) {
		String password = "A" + COMBINING_DIAERESIS + rest;

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(password);

		assertThat(refusal).contains(PasswordRefusal.BREAKS_THE_RULE);
	}

	@Test
	void countsAComposableMarkAfterALetterAsPartOfTheLetter() {
		String password = BASE + "e" + text(0x0301);

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(password);

		assertThat(refusal).contains(PasswordRefusal.BREAKS_THE_RULE);
	}

	@ParameterizedTest
	@ValueSource(strings = {"bcdef-1", "rger-123"})
	void acceptsADecomposedLetterWhenItsComposedFormMeetsTheRule(String rest) {
		String password = "A" + COMBINING_DIAERESIS + rest;

		Optional<PasswordRefusal> refusal = tested.refusalOfChoosing(password);

		assertThat(refusal).isEmpty();
	}

	@Test
	void refusesAReplacementThatIsThePasswordInAnotherNormalForm() {
		String composed = A_UMLAUT + "rger-Gilde9";
		String decomposed = "A" + COMBINING_DIAERESIS + "rger-Gilde9";

		Optional<PasswordRefusal> refusal = tested.refusalOfReplacing(composed, decomposed);

		assertThat(refusal).contains(PasswordRefusal.SAME_AS_BEFORE);
	}

	static Stream<Arguments> parityTable() {
		return Stream
				.of(arguments(BASE + "!", false), arguments(BASE + text(0x00A0), true), arguments(BASE + text(0x202F), true), arguments(BASE + text(0xFEFF), true),
						arguments(BASE + text(0x001C), true), arguments(BASE + text(0x1F600), false), arguments(BASE + COMBINING_DIAERESIS, true),
						arguments("A" + COMBINING_DIAERESIS + "bc-123", true), arguments("A" + COMBINING_DIAERESIS + "bcdefg1", true),
						arguments("A" + COMBINING_DIAERESIS + "bcdef-1", false), arguments(BASE + text(0x20AC), false), arguments(BASE + text(0x0085), true));
	}

	@ParameterizedTest
	@MethodSource("parityTable")
	void judgesTheParityTableTheSameAsThePage(String password, boolean refusedByThePage) {
		boolean refused = tested.refusalOfChoosing(password).isPresent();

		assertThat(refused).isEqualTo(refusedByThePage);
	}

	@ParameterizedTest
	@CsvSource({"Gilde-2026,Gilde-2027", "Gilde-2026,gilde-2026", "Gilde-2026,'Gilde-2026 '"})
	void acceptsAReplacementThatDiffersFromThePassword(String previous, String replacement) {
		Optional<PasswordRefusal> refusal = tested.refusalOfReplacing(previous, replacement);

		assertThat(refusal).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Gilde-2026", "Ärger-Ärger9"})
	void refusesAReplacementThatIsThePasswordItReplaces(String password) {
		Optional<PasswordRefusal> refusal = tested.refusalOfReplacing(password, password);

		assertThat(refusal).contains(PasswordRefusal.SAME_AS_BEFORE);
	}

	@ParameterizedTest
	@CsvSource({"Gilde-2026,short1!", "Gilde-2026,nodigits!!", "Gilde-2026,nospecial12"})
	void refusesAReplacementThatBreaksTheRule(String previous, String replacement) {
		Optional<PasswordRefusal> refusal = tested.refusalOfReplacing(previous, replacement);

		assertThat(refusal).contains(PasswordRefusal.BREAKS_THE_RULE);
	}
}
