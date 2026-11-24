package dev.schoenberg.evergore.protocolParser.businessLogic.usage;

import java.util.Arrays;
import java.util.List;

public enum UsageKind {
	LOGIN,
	PAGE_LOAD,
	LOGOUT,
	VIEW_OVERVIEW,
	VIEW_BANK,
	VIEW_STORAGE,
	VIEW_EXPLANATION,
	VIEW_ADMIN,
	VIEW_PASSWORD_CHANGE,
	VIEW_NONE,
	LAST_COLUMN_SWITCH,
	OVERVIEW_SORT,
	LEDGER_SORT,
	LEDGER_PAGE,
	NOTICE_UNDERSTOOD;

	public static List<UsageKind> uses() {
		return Arrays.stream(values()).filter(kind -> kind != NOTICE_UNDERSTOOD).toList();
	}
}
