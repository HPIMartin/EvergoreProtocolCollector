package dev.schoenberg.evergore.protocolParser.rest.controller;

import static java.lang.System.*;
import static org.apache.commons.text.StringEscapeUtils.*;

import java.util.*;
import java.util.function.*;

import jakarta.inject.*;

@Singleton
public class OutputFormatter {
	public static final String NEWLINE = lineSeparator();

	public <T> String createTable(List<T> elements, List<Column<T>> columns) {
		StringBuilder sb = new StringBuilder();

		addRow(columns, sb, column -> column.headline);

		elements.forEach(e -> addRow(columns, sb, column -> column.extractor.apply(e)));

		return sb.toString();
	}

	private <T> void addRow(List<Column<T>> columns, StringBuilder sb, Function<Column<T>, String> columnValue) {
		sb.append(NEWLINE).append("<tr>").append(NEWLINE);
		for (Column<T> column : columns) {
			addColumn(sb, columnValue.apply(column));
		}
		sb.append(NEWLINE).append("</tr>").append(NEWLINE);
	}

	private void addColumn(StringBuilder sb, String columnContent) {
		sb.append("<th>");
		sb.append(escapeHtml4(columnContent));
		sb.append("</th>");
	}

	public static class Column<T> {
		public final String headline;
		public final Function<T, String> extractor;

		public Column(String headline, Function<T, String> extractor) {
			this.headline = headline;
			this.extractor = extractor;
		}
	}
}
