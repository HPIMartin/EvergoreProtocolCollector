package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.UsageCounts;
import dev.schoenberg.evergore.protocolParser.application.UsageFigures;
import dev.schoenberg.evergore.protocolParser.application.UsageReport;
import dev.schoenberg.evergore.protocolParser.application.UsageWindow;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.UsageWire;

import static org.assertj.core.api.Assertions.assertThat;

class AdminUsageControllerMappingTest {
	private static final UsageCounts NOTHING = new UsageCounts(0, 0, 0, 0, Optional.empty());
	private static final UsageWindow NO_ACCOUNTS = new UsageWindow(List.of(), NOTHING, NOTHING);

	@Test
	void answersTheRecordsLostSinceTheStartTheReportGives() {
		UsageReport report = new UsageReport(NO_ACCOUNTS, NO_ACCOUNTS, Optional.of(LocalDate.parse("2026-11-01")), 4);

		UsageWire answer = new AdminUsageController(figuresGiving(report)).usage();

		assertThat(answer.lostSinceStart()).isEqualTo(4);
	}

	private static UsageFigures figuresGiving(UsageReport report) {
		return new UsageFigures(null, null, null, null) {
			@Override
			public UsageReport report() {
				return report;
			}
		};
	}
}
