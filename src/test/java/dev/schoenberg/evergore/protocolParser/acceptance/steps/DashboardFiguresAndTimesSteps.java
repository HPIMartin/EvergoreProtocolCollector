package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview.Placed;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardFiguresAndTimesSteps {
	private final MemberBrowser browser;

	public DashboardFiguresAndTimesSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	private Placed rowOf(String member) {
		return overview()
				.placedMemberRows()
				.stream()
				.filter(placed -> placed.row().cells().getFirst().equals(member))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The overview lists no row for " + member));
	}

	private Overview overview() {
		return browser.read("read-overview.js", Overview.class);
	}

	private static int columnOf(List<String> headers, String header) {
		int column = headers.indexOf(header);
		assertThat(column).as("the overview's column " + header + " among " + headers).isNotNegative();
		return column;
	}
}
