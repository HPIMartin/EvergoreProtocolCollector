package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public record MemberLedger(String heading, String caption, List<String> headers, List<List<String>> rows, String emptyMessage, boolean hasPrevious, boolean hasNext) {
	public static final String MINUTE_COLUMN = "Zeitpunkt";

	public int columnOf(String header) {
		int column = headers.indexOf(header);
		assertThat(column).as("the ledger's column " + header + " among " + headers).isNotNegative();
		return column;
	}
}
