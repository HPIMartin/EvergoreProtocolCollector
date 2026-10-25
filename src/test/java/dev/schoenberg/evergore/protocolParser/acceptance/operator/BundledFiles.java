package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class BundledFiles {
	private static final Pattern DASHBOARD_FILE = Pattern.compile("\"(/assets/[^\"]+)\"");
	private static final Pattern LOGIN_PAGE_FILE = Pattern.compile("\"(/login/assets/[^\"]+)\"");

	private BundledFiles() {}

	public static String aFileTheDashboardLoads() {
		return firstFileIn("/static/ui/index.html", DASHBOARD_FILE);
	}

	public static String aFileTheLoginPageLoads() {
		return firstFileIn("/static/ui/login/login.html", LOGIN_PAGE_FILE);
	}

	private static String firstFileIn(String page, Pattern file) {
		try (InputStream bundled = BundledFiles.class.getResourceAsStream(page)) {
			if (bundled == null) {
				throw new IllegalStateException("The page " + page + " is not on the classpath");
			}
			Matcher found = file.matcher(new String(bundled.readAllBytes(), UTF_8));
			if (!found.find()) {
				throw new IllegalStateException("The page " + page + " loads no file");
			}
			return found.group(1);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
