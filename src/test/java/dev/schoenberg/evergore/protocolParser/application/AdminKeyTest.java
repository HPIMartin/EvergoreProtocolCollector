package dev.schoenberg.evergore.protocolParser.application;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.FixedRandom;
import dev.schoenberg.evergore.protocolParser.LoggerSpy;

import static org.assertj.core.api.Assertions.assertThat;

class AdminKeyTest {
	private static final String KEY_OF_ONES = Base64.getUrlEncoder().withoutPadding().encodeToString(onesOf(32));

	private static final String LINE = "Console admin key, valid until the next restart: ";

	private final LoggerSpy logger = new LoggerSpy();
	private final AdminKey tested = AdminKey.drawn(new FixedRandom());

	@Test
	void announcesTheKeyInOneLogLine() {
		tested.announceTo(logger);

		assertThat(logger.infoMessages()).containsExactly(LINE + KEY_OF_ONES);
	}

	@Test
	void drawsAKeyOf43CharactersInTheUrlAlphabetWithoutPaddingThatOpens() {
		AdminKey drawn = AdminKey.drawn(new SecureRandom());
		drawn.announceTo(logger);

		String key = logger.infoMessages().getFirst().substring(LINE.length());
		boolean opens = drawn.opens("admin", key);

		assertThat(key).matches("[A-Za-z0-9_-]{43}");
		assertThat(opens).isTrue();
	}

	@Test
	void drawsANewKeyEveryTime() {
		AdminKey.drawn(new SecureRandom()).announceTo(logger);
		AdminKey.drawn(new SecureRandom()).announceTo(logger);

		long distinctLines = logger.infoMessages().stream().distinct().count();

		assertThat(distinctLines).isEqualTo(2);
	}

	@ParameterizedTest
	@ValueSource(strings = {"admin", "ADMIN", "Admin", "aDmIn", "admin ", " Admin"})
	void opensWithTheKeyUnderTheNameAdminInAnyCapitals(String name) {
		boolean opens = tested.opens(name, KEY_OF_ONES);

		assertThat(opens).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Aurora", "Zephyr", "", "adm in", "administrator"})
	void opensUnderNoOtherName(String name) {
		boolean opens = tested.opens(name, KEY_OF_ONES);

		assertThat(opens).isFalse();
	}

	@Test
	void opensWithNoOtherPassword() {
		boolean opens = tested.opens("admin", "Sommer-2026!");

		assertThat(opens).isFalse();
	}

	@Test
	void opensWithTheKeyOnlyInItsOwnCapitals() {
		boolean opens = tested.opens("admin", KEY_OF_ONES.toLowerCase());

		assertThat(opens).isFalse();
	}

	@Test
	void opensWithNeitherAPrefixNorAnExtensionOfTheKey() {
		boolean prefix = tested.opens("admin", KEY_OF_ONES.substring(0, 42));
		boolean extension = tested.opens("admin", KEY_OF_ONES + "A");

		assertThat(prefix).isFalse();
		assertThat(extension).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"admin", "ADMIN"})
	void knowsTheNameItIsBoundTo(String name) {
		boolean named = tested.isNamedBy(name);

		assertThat(named).isTrue();
	}

	@Test
	void isNotNamedByAnAvatar() {
		boolean named = tested.isNamedBy("Aurora");

		assertThat(named).isFalse();
	}

	private static byte[] onesOf(int length) {
		byte[] bytes = new byte[length];
		Arrays.fill(bytes, (byte) 1);
		return bytes;
	}

}
