package dev.schoenberg.evergore.protocolParser.helper.security;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRule;

import static org.assertj.core.api.Assertions.assertThat;

class RandomPasswordGeneratorTest {
	private static final int SAMPLES = 500;

	private final RandomPasswordGenerator tested = new RandomPasswordGenerator(new SecureRandom());

	@Test
	void generatesPasswordsOfSixteenCharacters() {
		Set<Integer> lengths = new HashSet<>();

		IntStream.range(0, SAMPLES).forEach(sample -> lengths.add(tested.generate().length()));

		assertThat(lengths).containsExactly(16);
	}

	@Test
	void generatesOnlyPasswordsThatPassThePasswordRule() {
		PasswordRule rule = new PasswordRule();

		long refused = IntStream.range(0, SAMPLES).mapToObj(sample -> tested.generate()).filter(generated -> rule.refusalOfChoosing(generated).isPresent()).count();

		assertThat(refused).isZero();
	}

	@Test
	void generatesADifferentPasswordEachTime() {
		Set<String> generated = new HashSet<>();

		IntStream.range(0, SAMPLES).forEach(sample -> generated.add(tested.generate()));

		assertThat(generated).hasSize(SAMPLES);
	}

	@Test
	void generatesNoWhitespaceAndNoCharacterThatNeedsQuoting() {
		Set<Character> used = new HashSet<>();

		IntStream.range(0, SAMPLES).forEach(sample -> tested.generate().chars().forEach(character -> used.add((char) character)));

		assertThat(used).noneMatch(Character::isWhitespace).doesNotContain('"', '\'', '\\', '<', '>', '&');
	}
}
