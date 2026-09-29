package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public record Overview(Roster active, Roster dormant, List<Stat> position, List<String> messages) {
	public Roster roster(RosterName name) {
		return name == RosterName.ACTIVE ? active : dormant;
	}

	public List<Row> memberRows() {
		return placedMemberRows().stream().map(Placed::row).toList();
	}

	public List<Placed> placedMemberRows() {
		return rosters().flatMap(roster -> roster.rows().stream().map(row -> new Placed(roster.headers(), row))).toList();
	}

	public List<Placed> placedGuildRows() {
		return rosters().filter(roster -> roster.total() != null).map(roster -> new Placed(roster.headers(), roster.total())).toList();
	}

	public Placed rowOf(String member) {
		return placedMemberRows()
				.stream()
				.filter(placed -> placed.row().cells().getFirst().equals(member))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The overview lists no row for " + member));
	}

	private Stream<Roster> rosters() {
		return Stream.of(active, dormant).filter(Objects::nonNull);
	}

	public record Roster(String caption, List<String> headers, String figureHeader, List<Row> rows, Row total, String emptyMessage, Sort sort) {}

	public record Row(List<String> cells, String mark, List<String> notes, List<String> hrefs) {}

	public record Placed(List<String> headers, Row row) {
		public int columnOf(String header) {
			int column = headers.indexOf(header);
			assertThat(column).as("the overview's column " + header + " among " + headers).isNotNegative();
			return column;
		}
	}

	public record Stat(String label, String value) {}
}
