package dev.schoenberg.evergore.protocolParser.domain;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public final class CatalogLookup {
	private static final Pattern MAGIC_AFFIX = Pattern.compile(" (?:des|der) \\p{Lu}\\p{L}+( \\[2H\\])?$");

	private CatalogLookup() {}

	public static Optional<EvergoreItem> itemFor(String ingameName) {
		return spellingsOf(ingameName).map(CatalogLookup::entryNamed).flatMap(Optional::stream).findFirst();
	}

	public static Stream<String> spellingsOf(String ingameName) {
		return Stream.of(ingameName, withoutMagicAffix(ingameName)).distinct();
	}

	private static Optional<EvergoreItem> entryNamed(String ingameName) {
		return stream(EvergoreItem.values()).filter(item -> item.isNamed(ingameName)).findAny();
	}

	private static String withoutMagicAffix(String ingameName) {
		return MAGIC_AFFIX.matcher(ingameName).replaceFirst("$1");
	}
}
