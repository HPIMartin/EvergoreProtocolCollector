package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.text.Normalizer;

public final class NormalForm {
	private NormalForm() {}

	public static String of(String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFC);
	}
}
