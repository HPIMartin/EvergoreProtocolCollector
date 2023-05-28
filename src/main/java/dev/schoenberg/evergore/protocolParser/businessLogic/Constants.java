package dev.schoenberg.evergore.protocolParser.businessLogic;

import java.time.*;

public class Constants {
	private Constants() {}

	public static final String GROUP_NAME_TYPE = "type";
	public static final String GROUP_NAME_AVATAR = "avatar";
	public static final String GROUP_NAME_DATE = "date";
	public static final String LAGER_EINTRAG_START = "^(?<" + GROUP_NAME_DATE + ">\\d{2}\\.\\d{2}.\\d{4} \\d{2}:\\d{2})(?<" + GROUP_NAME_AVATAR + ">.*)(?<"
			+ GROUP_NAME_TYPE + ">Einlagerung|Entnahme|Einzahlung).*";
	public static final String SERVER = "https://evergore.de";

	public static final ZoneId APP_ZONE = ZoneId.of("Europe/Berlin");
}
