package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;
import static dev.schoenberg.evergore.protocolParser.dataExtraction.parser.EntryFactory.*;

import java.util.*;

import dev.schoenberg.evergore.protocolParser.domain.*;

public class EntityParser {

	public static List<Entry> parse(List<String> content) {
		List<Integer> entryBeginnings = findEntries(content);
		return loadEntries(content, entryBeginnings);
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
