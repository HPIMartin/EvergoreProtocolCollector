package dev.schoenberg.evergore.protocolParser;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.*;
import static dev.schoenberg.evergore.protocolParser.dataExtraction.parser.EntryFactory.*;
import static java.time.LocalDateTime.*;
import static java.util.Arrays.*;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.ArrayList;

import dev.schoenberg.evergore.protocolParser.CsvParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.domain.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;
import dev.schoenberg.evergore.protocolParser.helper.logger.*;
import dev.schoenberg.evergore.protocolParser.rest.controller.*;

public class Main {
	private static Configuration config;

	public static void main(String[] args) throws Exception {
		config = new Configuration();

		// parseCSV();

		// test2();

		// test();

		// FileLoader fileLoader = new AlternativeFileLoaderWrapper(new DiscFileLoader(config), new ResourceFileLoader());
		//
		// System.out.println("Loading page content...");
		// PageContents pageContents = new PageContentExtractor(config, fileLoader).load();
		//
		// System.out.println("Interpreting content...");
		// String bankContent = parse(pageContents.bank);
		// String lagerContent = parse(pageContents.lager);
		//
		// System.out.println("Write content to disk: " + config.evergoreFolder);
		// FileWriter writer = new DiskFileWriter(config);
		// writer.write(bankContent, "bank.csv");
		// writer.write(lagerContent, "lager.csv");

		System.out.println("Done!");
	}

	@SuppressWarnings("unused")
	private static void parseCSV() throws IOException {
		List<ItemDto> parseCsv = new CsvParser().parseCsv("H:\\OneDrive\\Dokumente\\test.csv");

		Comparator<ItemDto> comparator = Comparator.comparingInt(ItemDto::getIntegerValue).thenComparing(Comparator.comparing(ItemDto::getStringValue));

		comparator = Comparator.comparing((ItemDto obj) -> {
			if ("HANDWERKSMATERIAL".equalsIgnoreCase(obj.getFirstString())) {
				return "0";
			}
			if ("Jagdbeuten".equalsIgnoreCase(obj.getFirstString())) {
				return "1";
			}
			if ("Rohstoffe".equalsIgnoreCase(obj.getFirstString())) {
				return "2";
			}
			if ("verarbeitete Rohstoffe".equalsIgnoreCase(obj.getFirstString())) {
				return "3";
			}
			if ("Edelsteine".equalsIgnoreCase(obj.getFirstString())) {
				return "4";
			}
			return obj.getFirstString();
		}).thenComparing(ItemDto::getSecondString);

		parseCsv.sort(comparator);

		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (ItemDto item : parseCsv) {
			sb.append(makeEnumValue(item.itemName));
			sb.append("(\"");
			sb.append(item.itemName);
			sb.append("\", ");
			sb.append(item.price);
			sb.append(", Category.");
			sb.append(item.category.replace(" ", "_").replace("\t", "_").replace("(", "").replace(")", "").replace("[", "").replace("]", "").replace("-", "_")
					.toUpperCase());
			sb.append(", ");

			if (item.amount == 0) {
				sb.append("Recipe.NOT_CRAFTABLE");
			} else {
				sb.append("new Recipe(");
				sb.append(item.amount);

				first = true;
				for (IngredientDto ing : item.ingredients) {
					if (first) {
						first = false;
					}
					sb.append(", ");
					sb.append("new Ingredient(");
					sb.append(ing.amount);
					sb.append(", EvergoreItem.");
					sb.append(makeEnumValue(ing.item));
					sb.append(")");
				}

				sb.append(")");
			}

			sb.append("),\n");
		}

		System.out.println(sb.toString());
		Files.writeString(Path.of("H:\\OneDrive\\Dokumente\\test.txt"), sb.toString());
	}

	private static String makeEnumValue(String itemName) {
		return itemName.toUpperCase().replace("Ä", "AE").replace("Ü", "UE").replace("Ö", "OE").replace("ß", "SS").replace("[", "").replace("]", "")
				.replace(' ', '_').replace("-", "_");
	}

	@SuppressWarnings("unused")
	private static void test2() throws Exception {
		Logger logger = new Slf4jLogger();
		AvatarController controller = new AvatarController(BankDatabaseRepository.get(config, logger, () -> {}), null, new OutputFormatter(), logger);
		String content = controller.bankInformation("Gorim", 0);
		System.out.println(content);
	}

	@SuppressWarnings("unused")
	private static void test() throws SQLException, Exception {
		BankEntry entry = new BankEntry(of(2101, 4, 10, 13, 37).atZone(APP_ZONE).toInstant(), "Alessia", 42, EINLAGERUNG);
		BankEntry entry2 = new BankEntry(of(1990, 1, 1, 0, 0).atZone(APP_ZONE).toInstant(), "not(Alessia)", 42, EINLAGERUNG);

		BankDatabaseRepository repo = BankDatabaseRepository.get(config, new Slf4jLogger(), () -> {});
		repo.add(asList(entry, entry2));

		BankEntry newest = repo.getNewest();
		List<BankEntry> alessias = repo.getAllFor("Alessia", 0L, 100L);

		System.out.println("Newest: " + newest.equals(entry));
		System.out.println("Alessias: " + (alessias.size() == 1 && alessias.get(0).equals(entry)));

		List<BankEntry> allAfter = repo.getAllFor("Alessia", LocalDateTime.of(2101, 4, 10, 13, 37, 00));
		System.out.println("Size: " + allAfter.size());
		System.out.println(allAfter.get(0).stupidMerge());
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
			if (oldBegining != null) {
				List<String> entryContent = lines.subList(oldBegining, beginning);
				result.add(parseContent(entryContent));
			}
			oldBegining = beginning;
		}
		if (oldBegining != null) {
			result.add(parseContent(lines.subList(oldBegining, lines.size())));
		}
		return result;
	}
}
