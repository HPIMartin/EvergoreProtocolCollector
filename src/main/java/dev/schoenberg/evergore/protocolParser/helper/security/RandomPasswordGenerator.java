package dev.schoenberg.evergore.protocolParser.helper.security;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import dev.schoenberg.evergore.protocolParser.application.PasswordGenerator;

public class RandomPasswordGenerator implements PasswordGenerator {
	private static final int LENGTH = 16;
	private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
	private static final String DIGITS = "23456789";
	private static final String SPECIALS = "!#$%*+-=?@_";
	private static final String ANY = LETTERS + DIGITS + SPECIALS;

	private final SecureRandom random;

	public RandomPasswordGenerator(SecureRandom random) {
		this.random = random;
	}

	@Override
	public String generate() {
		List<Character> characters = new ArrayList<>();
		characters.add(oneOf(DIGITS));
		characters.add(oneOf(SPECIALS));
		while (characters.size() < LENGTH) {
			characters.add(oneOf(ANY));
		}
		Collections.shuffle(characters, random);

		return characters.stream().map(String::valueOf).collect(Collectors.joining());
	}

	private char oneOf(String alphabet) {
		return alphabet.charAt(random.nextInt(alphabet.length()));
	}
}
