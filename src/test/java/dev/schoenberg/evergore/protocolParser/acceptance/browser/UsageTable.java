package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record UsageTable(String window, List<String> headers, List<UsageTableRow> rows) {
	public static final String ACCOUNT = "usage-row";
	public static final String GUILD = "usage-guild";
	public static final String ADMIN_KEY = "usage-admin-key";

	public List<String> rowOf(String kind, String name) {
		return rows
				.stream()
				.filter(row -> row.kind().equals(kind) && row.cells().getFirst().equals(name))
				.map(UsageTableRow::cells)
				.findFirst()
				.orElseThrow(() -> new AssertionError("the usage table shows no row of " + name + ": " + rows));
	}

	public String cellOf(List<String> row, String column) {
		int index = headers.indexOf(column);
		if (index < 0) {
			throw new AssertionError("the usage table has no column " + column + ": " + headers);
		}
		return row.get(index);
	}

	public List<String> columnsOf(List<String> row, List<String> columns) {
		return columns.stream().map(column -> cellOf(row, column)).toList();
	}

	public record UsageTableRow(String kind, List<String> cells) {}
}
