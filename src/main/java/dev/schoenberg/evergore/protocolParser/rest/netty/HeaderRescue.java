package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

final class HeaderRescue {
	private static final byte LINE_FEED = '\n';
	private static final String CARRIAGE_RETURN_LINE_FEED = "\r\n";
	private static final Pattern LINE_END = Pattern.compile("\r?\n$");

	private final int budget;
	private final List<RescuedHeader> headers = new ArrayList<>();
	private final StringBuilder lineSoFar = new StringBuilder();
	private boolean skippingRejectedLine;
	private boolean complete;
	private int used;

	private HeaderRescue(int budget, boolean skippingRejectedLine, boolean complete) {
		this.budget = budget;
		this.skippingRejectedLine = skippingRejectedLine;
		this.complete = complete;
	}

	static HeaderRescue after(SliceEnd failureSlice, int budget) {
		return new HeaderRescue(budget, failureSlice == SliceEnd.MID_LINE, failureSlice == SliceEnd.END_OF_HEAD);
	}

	void feed(byte[] slice) {
		if (complete) {
			return;
		}

		used += slice.length;
		if (used > budget) {
			complete = true;
			return;
		}
		if (skippingRejectedLine) {
			skippingRejectedLine = !endsLine(slice);
			return;
		}

		lineSoFar.append(new String(slice, ISO_8859_1));
		if (endsLine(slice)) {
			readLine();
		}
	}

	void finishTheRejectedLine(byte[] startOfIt) {
		if (!skippingRejectedLine || startOfIt.length == 0) {
			return;
		}

		skippingRejectedLine = false;
		used += startOfIt.length;
		lineSoFar.append(new String(startOfIt, ISO_8859_1));
	}

	void alsoRead(byte[] rejectedLine) {
		addIfHeaderLine(new String(rejectedLine, ISO_8859_1));
	}

	boolean isComplete() {
		return complete;
	}

	List<RescuedHeader> headers() {
		return List.copyOf(headers);
	}

	private void readLine() {
		String rawLine = lineSoFar.toString();
		lineSoFar.setLength(0);
		if (withoutLineEnd(rawLine).isEmpty()) {
			complete = true;
			return;
		}

		addIfHeaderLine(rawLine);
	}

	private void addIfHeaderLine(String rawLine) {
		if (!rawLine.endsWith(CARRIAGE_RETURN_LINE_FEED)) {
			return;
		}

		String line = withoutLineEnd(rawLine);
		int colon = line.indexOf(':');
		String name = line.substring(0, Math.max(colon, 0));
		if (!name.isEmpty() && name.chars().noneMatch(Character::isWhitespace)) {
			headers.add(new RescuedHeader(name, withoutOptionalWhitespace(line.substring(colon + 1))));
		}
	}

	private static boolean endsLine(byte[] slice) {
		return slice.length > 0 && slice[slice.length - 1] == LINE_FEED;
	}

	private static String withoutOptionalWhitespace(String value) {
		int start = 0;
		int end = value.length();
		while (start < end && isOptionalWhitespace(value.charAt(start))) {
			start++;
		}
		while (end > start && isOptionalWhitespace(value.charAt(end - 1))) {
			end--;
		}
		return value.substring(start, end);
	}

	private static boolean isOptionalWhitespace(char character) {
		return character == ' ' || character == '\t';
	}

	private static String withoutLineEnd(String line) {
		return LINE_END.matcher(line).replaceFirst("");
	}
}
