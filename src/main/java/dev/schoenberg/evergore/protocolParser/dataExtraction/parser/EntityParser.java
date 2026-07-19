package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import java.util.ArrayList;
import java.util.List;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.domain.Entry;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.LAGER_EINTRAG_BOUNDARY;
import static dev.schoenberg.evergore.protocolParser.dataExtraction.parser.EntryFactory.parseContent;

public class EntityParser {

	public static List<Entry> parse(List<String> content, Logger logger) {
		List<Integer> entryBeginnings = findEntries(content);
		return loadEntries(content, entryBeginnings, logger);
	}

	private static List<Integer> findEntries(List<String> lines) {
		List<Integer> result = new ArrayList<>();
		int lineCounter = 0;
		for (String line : lines) {
			if (line.matches(LAGER_EINTRAG_BOUNDARY)) {
				result.add(lineCounter);
			}
			lineCounter++;
		}
		return result;
	}

	private static List<Entry> loadEntries(List<String> lines, List<Integer> entryBeginnings, Logger logger) {
		List<Entry> result = new ArrayList<>();
		Integer previousBeginning = null;
		for (int beginning : entryBeginnings) {
			if (previousBeginning == null) {
				previousBeginning = beginning;
			} else {
				List<String> entryContent = lines.subList(previousBeginning, beginning);
				parseContent(entryContent, logger).ifPresent(result::add);
				previousBeginning = beginning;
			}
		}
		if (previousBeginning != null) {
			parseContent(lines.subList(previousBeginning, lines.size()), logger).ifPresent(result::add);
		}
		return result;
	}
}
