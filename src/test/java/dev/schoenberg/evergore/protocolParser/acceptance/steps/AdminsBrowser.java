package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.acceptance.world.UsageActors;

public class AdminsBrowser {
	private static final String ADMIN = "/admin";

	private final MemberBrowser browser;
	private final UsageActors actors;
	private final ScenarioTime time;
	private final RunningService service;

	public AdminsBrowser(MemberBrowser browser, UsageActors actors, ScenarioTime time, RunningService service) {
		this.service = service;
		this.browser = browser;
		this.actors = actors;
		this.time = time;
	}

	public void openTheAdminPage() {
		service.awaitTheUsageWritten();
		actors.adminsBrowserWithALogin().ifPresentOrElse(admins -> admins.openHref(ADMIN), () -> {
			browser.opening().openAsTheAdmin(ADMIN);
			actors.adminLoggedIn(time.now(), browser);
		});
	}

	public MemberBrowser current() {
		return actors.adminsBrowserWithALogin().orElse(browser);
	}
}
