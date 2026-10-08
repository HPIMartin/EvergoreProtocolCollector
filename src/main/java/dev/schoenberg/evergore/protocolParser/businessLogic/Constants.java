package dev.schoenberg.evergore.protocolParser.businessLogic;

import java.time.ZoneId;
import java.util.List;

public class Constants {
	private Constants() {}

	public static final String GROUP_NAME_TYPE = "type";
	public static final String GROUP_NAME_AVATAR = "avatar";
	public static final String GROUP_NAME_DATE = "date";
	public static final String ENTNAHME_TYPE_WORD = "Entnahme";
	public static final List<String> TRANSFER_TYPE_WORDS = List.of("Einlagerung", ENTNAHME_TYPE_WORD, "Einzahlung");
	public static final String LAGER_EINTRAG_START = "^(?<" + GROUP_NAME_DATE + ">\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2})(?<" + GROUP_NAME_AVATAR + ">.*)\\s(?<" + GROUP_NAME_TYPE
			+ ">" + String.join("|", TRANSFER_TYPE_WORDS) + ")(?=\\s|$).*";
	private static final String ANY_SEPARATOR = ".";
	private static final String NON_ITEM_SEPARATOR = "[^\\d ]";
	private static final String TWO_DIGIT_TIMESTAMP = "\\d{2}" + ANY_SEPARATOR + "\\d{2}" + ANY_SEPARATOR + "\\d{4} \\d{2}:\\d{2}";
	private static final String ONE_OR_TWO_DIGIT_TIMESTAMP = "\\d{1,2}" + NON_ITEM_SEPARATOR + "\\d{1,2}" + ANY_SEPARATOR + "\\d{4} \\d{1,2}:\\d{1,2}";
	public static final String LAGER_EINTRAG_BOUNDARY = "^(?:" + TWO_DIGIT_TIMESTAMP + "|" + ONE_OR_TWO_DIGIT_TIMESTAMP + ").*";
	public static final String SERVER = "https://evergore.de";

	public static final ZoneId APP_ZONE = ZoneId.of("Europe/Berlin");
}
