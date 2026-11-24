package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;

import dev.schoenberg.evergore.protocolParser.OrphanedBrowserSweep;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Browsers;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.SecondBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioDatabase;

public class ServiceHooks {
	private final RunningService service;
	private final MemberBrowser browser;
	private final SecondBrowser secondBrowser;
	private final ScenarioDatabase database;
	private final GameProtocols protocols;

	public ServiceHooks(RunningService service, MemberBrowser browser, SecondBrowser secondBrowser, ScenarioDatabase database, GameProtocols protocols) {
		this.protocols = protocols;
		this.service = service;
		this.browser = browser;
		this.secondBrowser = secondBrowser;
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
			try {
				try {
					browser.leave();
				} finally {
					secondBrowser.browser().leave();
				}
			} finally {
				protocols.release();
				service.awaitTheCollectionsEnded();
			}
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
