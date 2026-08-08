package dev.schoenberg.evergore.protocolParser;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.DefaultTaskExceptionHandler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Browser;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@MicronautTest
class DashboardBrowserSmokeTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/dashboardBrowser/dashboardBrowser.sqlite");
	private static final Duration RENDER_GUARD = Duration.ofSeconds(30);
	private static final By DATA_ROW = By.cssSelector("[data-testid='data-row']");

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = DashboardBrowserSmokeTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject BootSignalRecorder signals;
	private @Inject Configuration config;

	@BeforeEach
	void setup() {
		assumeTrue(browserIsOnPath(), "no firefox on the PATH: browser-driven check skipped");
		signals.awaitCollection();
	}

	@Test
	void theOverviewShowsEveryAvatarWithItsRecomputedBankTotals() {
		List<List<String>> rows = renderedRowsOf("/overview");

		assertThat(rows).containsExactly(List.of("Aurora", "200", "1.500", "öffnen"), List.of("Boreas", "0", "750", "öffnen"), List.of("Calix", "300", "0", "öffnen"));
	}

	@Test
	void aBankDeepLinkShowsThatAvatarsEntriesNewestFirst() {
		List<List<String>> rows = renderedRowsOf("/avatars/Aurora/bank");

		assertThat(rows)
				.containsExactly(List.of("12.01.2024 12:00", "Aurora", "200", "Entnahme"), List.of("11.01.2024 11:00", "Aurora", "500", "Einlagerung"),
						List.of("10.01.2024 10:00", "Aurora", "1.000", "Einlagerung"));
	}

	private List<List<String>> renderedRowsOf(String clientRoute) {
		WebDriver driver = Browser.fromString(config.browser).getDriver(config);
		try {
			driver.get("http://localhost:" + server.getPort() + clientRoute + "?token=test-token");
			new WebDriverWait(driver, RENDER_GUARD).until(browser -> !browser.findElements(DATA_ROW).isEmpty());

			return driver.findElements(DATA_ROW).stream().map(DashboardBrowserSmokeTest::cellsOf).toList();
		} finally {
			driver.quit();
		}
	}

	private static List<String> cellsOf(WebElement row) {
		return row.findElements(By.tagName("td")).stream().map(WebElement::getText).toList();
	}

	private static boolean browserIsOnPath() {
		String path = System.getenv("PATH");
		if (path == null) {
			return false;
		}
		return stream(path.split(File.pathSeparator)).anyMatch(DashboardBrowserSmokeTest::containsABrowser);
	}

	private static boolean containsABrowser(String directory) {
		return new File(directory, "firefox").canExecute() || new File(directory, "firefox-esr").canExecute();
	}

	@MockBean(Configuration.class)
	Configuration configurationMock() {
		return new TestConfiguration();
	}

	public static class TestConfiguration extends Configuration {
		@Override
		public String getDatabasePath() {
			return WORKING_DB.toString();
		}

		@Override
		public int getCollectorInitialDelaySeconds() {
			return 0;
		}
	}

	@MockBean(EvergoreDataExtractor.class)
	TestEvergoreDataExtractor testEvergoreDataExtractor() {
		return new TestEvergoreDataExtractor();
	}

	public static class TestEvergoreDataExtractor extends EvergoreDataExtractor {
		public TestEvergoreDataExtractor() {
			super(null, null, null, null);
		}

		@Override
		public void loadData() {}
	}

	@MockBean(PreDatabaseConnectionHook.class)
	PreDatabaseConnectionHook databaseHook() {
		return () -> {};
	}

	@MockBean(PostCollectionHook.class)
	PostCollectionHook collectionHook(BootSignalRecorder recorder) {
		return recorder::recordCollectionFinished;
	}

	@MockBean(DefaultTaskExceptionHandler.class)
	DefaultTaskExceptionHandler exceptionHandler(BootSignalRecorder recorder) {
		return new TestTaskExceptionHandler(recorder);
	}

	public static class TestTaskExceptionHandler extends DefaultTaskExceptionHandler {
		private final BootSignalRecorder signals;

		public TestTaskExceptionHandler(BootSignalRecorder signals) {
			this.signals = signals;
		}

		@Override
		public void handle(Object bean, Throwable throwable) {
			signals.recordException();
		}
	}
}
