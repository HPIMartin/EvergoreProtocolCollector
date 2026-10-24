package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;

public class PasswordRule {
	private static final int MINIMUM_LENGTH = 8;
	private static final Set<Integer> SPECIAL_TYPES = Set
			.of((int) Character.CONNECTOR_PUNCTUATION, (int) Character.DASH_PUNCTUATION, (int) Character.START_PUNCTUATION, (int) Character.END_PUNCTUATION,
					(int) Character.INITIAL_QUOTE_PUNCTUATION, (int) Character.FINAL_QUOTE_PUNCTUATION, (int) Character.OTHER_PUNCTUATION, (int) Character.MATH_SYMBOL,
					(int) Character.CURRENCY_SYMBOL, (int) Character.MODIFIER_SYMBOL, (int) Character.OTHER_SYMBOL);

	public Optional<PasswordRefusal> refusalOfChoosing(String password) {
		String chosen = NormalForm.of(password);
		if (chosen.codePointCount(0, chosen.length()) < MINIMUM_LENGTH || !hasCodePoint(chosen, Character::isDigit) || !hasCodePoint(chosen, PasswordRule::isSpecial)) {
			return Optional.of(PasswordRefusal.BREAKS_THE_RULE);
		}
		return Optional.empty();
	}

	public Optional<PasswordRefusal> refusalOfReplacing(String previous, String replacement) {
		Optional<PasswordRefusal> brokenRule = refusalOfChoosing(replacement);
		if (brokenRule.isPresent()) {
			return brokenRule;
		}
		if (NormalForm.of(previous).equals(NormalForm.of(replacement))) {
			return Optional.of(PasswordRefusal.SAME_AS_BEFORE);
		}
		return Optional.empty();
	}

	private static boolean hasCodePoint(String password, IntPredicate kind) {
		return password.codePoints().anyMatch(kind);
	}

	private static boolean isSpecial(int codePoint) {
		return SPECIAL_TYPES.contains(Character.getType(codePoint));
	}
}
