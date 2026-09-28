package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;

import dev.schoenberg.evergore.protocolParser.OrphanedBrowserSweep;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Browsers;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioDatabase;

public class ServiceHooks {
	private final RunningService service;
	private final MemberBrowser browser;
	private final ScenarioDatabase database;

	public ServiceHooks(RunningService service, MemberBrowser browser, ScenarioDatabase database) {
		this.service = service;
		this.browser = browser;
		this.database = database;
	}

	@BeforeAll
	public static void sweepBrowsersEarlierRunsLeftBehind() {
		OrphanedBrowserSweep.sweep();
	}

	@Before
	public void startTheService() {
		database.prepare();
		service.start();
	}

	@After
	public void stopTheService() {
		try {
			browser.leave();
		} finally {
			service.stop();
			database.delete();
		}
	}

	@AfterAll
	public static void quitTheBrowsers() {
		Browsers.quitAll();
	}
}
