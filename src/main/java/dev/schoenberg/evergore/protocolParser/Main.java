package dev.schoenberg.evergore.protocolParser;

import static java.util.Arrays.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.entry.Entry;
import dev.schoenberg.evergore.protocolParser.entry.EntryFactory;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.resourceFiles.ResourceFileLoader;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Driver;

public class Main {
	public static final String GROUP_NAME_TYPE = "type";
	public static final String GROUP_NAME_AVATAR = "avatar";
	public static final String GROUP_NAME_DATE = "date";
	public static final String LAGER_EINTRAG_START = "^(?<" + GROUP_NAME_DATE + ">\\d{2}\\.\\d{2}.\\d{4} \\d{2}:\\d{2})(?<" + GROUP_NAME_AVATAR + ">.*)(?<"
			+ GROUP_NAME_TYPE + ">Einlagerung|Entnahme|Einzahlung).*";
	private static final String SERVER = "https://evergore.de";
	private static Configuration config;

	public static void main(String[] args) throws Exception {
		config = new Configuration();

		List<String> content;
		if (config.devMode) {
			content = Files.readAllLines(Paths.get("src", "main", "resources", "toBeParsed.txt"));
		} else {
			content = load();
		}
		String result = parse(content);
		copyToClipboard(result);
		if (config.devMode) {
			System.out.println(content.stream().reduce((x, y) -> x + "\n" + y).orElse(""));
		}
	}

	private static void copyToClipboard(String result) {
		StringSelection selection = new StringSelection(result);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}

	private static List<String> load() throws IOException {
		List<String> content = new ArrayList<>();

		WebDriver driver = new Driver(new ResourceFileLoader(), config).createWebDriver();

		loadEvergore(driver);
		for (int page = 1; page <= config.numberOfPages; page++) {
			String text = loadStoragePage(driver, page);
			content.addAll(asList(text.split("\n")));
		}

		driver.close();

		return content;
	}

	private static String loadStoragePage(WebDriver driver, int page) {
		String protocol = "guild_protocol&selection=2"; // Bank
		if (config.parseStorage) {
			protocol = "town_protocol&selection=3"; // Lager
		}
		String url = SERVER + "/" + config.server + "?page=" + protocol + "&pos=" + page;
		driver.navigate().to(url);
		wait(driver, url);
		return driver.findElement(By.tagName("body")).getText();
	}

	private static void loadEvergore(WebDriver driver) {
		driver.navigate().to(SERVER + "/login");
		wait(driver, SERVER + "/" + config.server);
	}

	private static void wait(WebDriver driver, String url) {
		System.out.println("Waiting for: " + url);
		new WebDriverWait(driver, 300).until(urlToBe(url));
	}

	private static String parse(List<String> content) {
		List<Integer> entryBeginnings = findEntries(content);
		List<Entry> entries = loadEntries(content, entryBeginnings);
		List<String> result = new ArrayList<>();
		for (Entry entry : entries) {
			result.add(entry.print());
		}
		return result.stream().reduce((x, y) -> x + "\n" + y).orElse("");
	}

	private static List<Integer> findEntries(List<String> lines) {
		List<Integer> result = new ArrayList<>();
		int lineCounter = 0;
		for (String line : lines) {
			if (line.matches(LAGER_EINTRAG_START)) {
				result.add(lineCounter);
			}
			lineCounter++;
		}
		return result;
	}

	private static List<Entry> loadEntries(List<String> lines, List<Integer> entryBeginnings) {
		List<Entry> result = new ArrayList<>();
		Integer oldBegining = null;
		for (int beginning : entryBeginnings) {
			if (oldBegining == null) {
				oldBegining = beginning;
			} else {
				List<String> entryContent = lines.subList(oldBegining, beginning);
				result.add(EntryFactory.parseContent(entryContent));
				oldBegining = beginning;
			}
		}
		result.add(EntryFactory.parseContent(lines.subList(oldBegining, lines.size())));
		return result;
	}
}
