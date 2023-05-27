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

		addRow(columns, sb, column -> column.headline, RowType.HEADLINE);

		elements.forEach(e -> addRow(columns, sb, column -> column.extractor.apply(e), RowType.DATA));

		return sb.toString();
	}

	private <T> void addRow(List<Column<T>> columns, StringBuilder sb, Function<Column<T>, String> columnValue, RowType type) {
		sb.append(NEWLINE).append("<tr>").append(NEWLINE);
		for (Column<T> column : columns) {
			addColumn(sb, columnValue.apply(column), type);
		}
		sb.append(NEWLINE).append("</tr>").append(NEWLINE);
	}

	private void addColumn(StringBuilder sb, String columnContent, RowType type) {
		sb.append("<" + type.markup + ">");
		sb.append(escapeHtml4(columnContent));
		sb.append("</" + type.markup + ">");
	}

	public record Column<T>(String headline, Function<T, String> extractor) {}

	private enum RowType {
		HEADLINE("th"), DATA("td");

		public final String markup;

		RowType(String markup) {
			this.markup = markup;
		}
	}
}
