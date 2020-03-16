package protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import protocolParser.entry.Entry;
import protocolParser.entry.EntryFactory;

public class Main {
	public static void main(String[] args) throws Exception {
		List<String> lines = Files.readAllLines(Paths.get("src", "main", "resources", "toBeParsed.txt"));
		List<Integer> entryBeginnings = findEntries(lines);
		List<Entry> entries = loadEntries(lines, entryBeginnings);
		for (Entry entry : entries) {
			entry.print();
		}
	}

	private static List<Integer> findEntries(List<String> lines) {
		List<Integer> result = new ArrayList<>();
		int lineCounter = 0;
		for (String line : lines) {
			if (line.matches("^\\d{2}\\.\\d{2}.\\d{4} \\d{2}:\\d{2}.*")) {
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
