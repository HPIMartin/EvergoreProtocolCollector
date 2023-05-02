package dev.schoenberg.evergore.protocolParser.rest.controller;

import static java.lang.Math.*;
import static java.lang.String.*;
import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.function.*;

import jakarta.inject.*;

@Singleton
public class OutputFormatter {
	public <T> String generateOutput(List<T> data, List<Column<T>> columns) {

		StringBuilder sb = new StringBuilder();
		List<ColumnConfig<T>> sizes = getSizes(data, columns);

		for (OutputFormatter.ColumnConfig<T> conf : sizes) {
			sb.append(format("%-" + conf.length + "s", conf.column.headline));
		}
		sb.append("\n");

		for (T dto : data) {
			for (OutputFormatter.ColumnConfig<T> conf : sizes) {
				sb.append(format("%-" + conf.length + "s", conf.column.extractor.apply(dto)));
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private <T> List<ColumnConfig<T>> getSizes(List<T> data, List<Column<T>> columns) {
		return columns.stream().map(c -> new ColumnConfig<>(c, getMaxLength(data, c))).collect(toList());
	}

	public static <T> int getMaxLength(List<T> dtos, Column<T> column) {
		int longestValue = dtos.stream().map(column.extractor).mapToInt(String::length).max().orElse(0);
		int headerLength = column.headline.length();
		return max(max(longestValue, headerLength) + 10, 20);
	}

	public static class Column<T> {
		public final String headline;
		public final Function<T, String> extractor;

		public Column(String headline, Function<T, String> extractor) {
			this.headline = headline;
			this.extractor = extractor;
		}
	}

	private static class ColumnConfig<T> {
		public final Column<T> column;
		public final int length;

		public ColumnConfig(Column<T> column, int length) {
			this.column = column;
			this.length = length;
		}
	}
}