package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.UsageCounts;
import dev.schoenberg.evergore.protocolParser.application.UsageFigures;
import dev.schoenberg.evergore.protocolParser.application.UsageReport;
import dev.schoenberg.evergore.protocolParser.application.UsageWindow;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AccountUsageWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.UsageCountsWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.UsageWindowWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.UsageWire;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(AdminUsageController.PATH)
public class AdminUsageController {
	public static final String PATH = "/api/v1/admin/usage";

	private final UsageFigures figures;

	public AdminUsageController(UsageFigures figures) {
		this.figures = figures;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public UsageWire usage() {
		UsageReport report = figures.report();
		return new UsageWire(wireOf(report.lastThirtyDays()), wireOf(report.allTheTime()), report.recordedSince().orElse(null), report.lostSinceStart());
	}

	private static UsageWindowWire wireOf(UsageWindow window) {
		return new UsageWindowWire(window.accounts().stream().map(account -> new AccountUsageWire(account.name(), wireOf(account.counts()))).toList(), wireOf(window.guild()),
				wireOf(window.adminKey()));
	}

	private static UsageCountsWire wireOf(UsageCounts counts) {
		return new UsageCountsWire(counts.logins(), counts.pageLoads(), counts.logouts(), counts.daysUsed(), counts.latestUse().orElse(null));
	}
}
