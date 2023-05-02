package dev.schoenberg.evergore.protocolParser;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.*;
import static dev.schoenberg.evergore.protocolParser.dataExtraction.parser.EntryFactory.*;
import static java.time.LocalDateTime.of;
import static java.time.ZoneOffset.*;
import static java.util.Arrays.*;

import java.sql.*;
import java.util.*;
import java.util.ArrayList;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.domain.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;
import dev.schoenberg.evergore.protocolParser.helper.logger.*;

public class Main {
	private static Configuration config;

	public static void main(String[] args) throws Exception {
		config = new Configuration();

		test();

//		FileLoader fileLoader = new AlternativeFileLoaderWrapper(new DiscFileLoader(config), new ResourceFileLoader());
//
//		System.out.println("Loading page content...");
//		PageContents pageContents = new PageContentExtractor(config, fileLoader).load();
//
//		System.out.println("Interpreting content...");
//		String bankContent = parse(pageContents.bank);
//		String lagerContent = parse(pageContents.lager);
//
//		System.out.println("Write content to disk: " + config.evergoreFolder);
//		FileWriter writer = new DiskFileWriter(config);
//		writer.write(bankContent, "bank.csv");
//		writer.write(lagerContent, "lager.csv");

		System.out.println("Done!");
	}

	private static void test() throws SQLException, Exception {
		BankEntry entry = new BankEntry(of(1990, 4, 10, 13, 37).toInstant(UTC), "Alessia", 42, Einlagerung);
		BankEntry entry2 = new BankEntry(of(1990, 1, 1, 0, 0).toInstant(UTC), "not(Alessia)", 42, Einlagerung);

		BankDatabaseRepository repo = BankDatabaseRepository.get(config, new Slf4jLogger());
		repo.add(asList(entry, entry2));

		BankEntry newest = repo.getNewest();
		List<BankEntry> alessias = repo.getAllFor("Alessia");

		System.out.println("Newest: " + newest.equals(entry));
		System.out.println("Alessias: " + (alessias.size() == 1 && alessias.get(0).equals(entry)));
	}

	@SuppressWarnings("unused")
	private static String parse(List<String> content) {
		List<Integer> entryBeginnings = findEntries(content);
		List<Entry> entries = loadEntries(content, entryBeginnings);
		List<String> result = new ArrayList<>();
		for (Entry entry : entries) {
			result.add(entry.print(";"));
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
				result.add(parseContent(entryContent));
				oldBegining = beginning;
			}
		}
		if (oldBegining != null) {
			result.add(parseContent(lines.subList(oldBegining, lines.size())));
		}
		return result;
	}
}
