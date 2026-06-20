package dev.schoenberg.evergore.protocolParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenderedTable {
	private static final Pattern ROW_PATTERN = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern CELL_PATTERN = Pattern.compile("<t[hd][^>]*>(.*?)</t[hd]>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

	private final List<String> header;
	private final List<List<String>> rows;

	private RenderedTable(List<String> header, List<List<String>> rows) {
		this.header = header;
		this.rows = rows;
	}

	public static RenderedTable parse(String html) {
		List<List<String>> allRows = new ArrayList<>();
		Matcher rowMatcher = ROW_PATTERN.matcher(html);
		while (rowMatcher.find()) {
			allRows.add(extractCells(rowMatcher.group(1)));
		}
		if (allRows.isEmpty()) {
			return new RenderedTable(List.of(), List.of());
		}
		List<String> header = allRows.get(0);
		List<List<String>> dataRows = allRows.subList(1, allRows.size());
		return new RenderedTable(header, dataRows);
	}

	private static List<String> extractCells(String rowHtml) {
		List<String> cells = new ArrayList<>();
		Matcher cellMatcher = CELL_PATTERN.matcher(rowHtml);
		while (cellMatcher.find()) {
			cells.add(normalizeText(cellMatcher.group(1)));
		}
		return cells;
	}

	private static String normalizeText(String raw) {
		String stripped = TAG_PATTERN.matcher(raw).replaceAll("");
		String unescaped = stripped
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">")
				.replace("&quot;", "\"")
				.replace("&#39;", "'")
				.replace("&nbsp;", " ")
				.replace("&Auml;", "Ä")
				.replace("&auml;", "ä")
				.replace("&Ouml;", "Ö")
				.replace("&ouml;", "ö")
				.replace("&Uuml;", "Ü")
				.replace("&uuml;", "ü")
				.replace("&szlig;", "ß");
		return unescaped.trim().replaceAll("\\s+", " ");
	}

	public List<String> header() {
		return header;
	}

	public List<List<String>> rows() {
		return rows;
	}

	public List<String> rowForFirstCell(String firstCellValue) {
		return rows.stream().filter(r -> !r.isEmpty() && r.get(0).equals(firstCellValue)).findFirst().orElse(List.of());
	}
}
