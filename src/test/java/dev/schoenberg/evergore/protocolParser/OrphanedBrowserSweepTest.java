package dev.schoenberg.evergore.protocolParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.OrphanedBrowserSweep.failOnAnyLeftover;
import static dev.schoenberg.evergore.protocolParser.OrphanedBrowserSweep.sweepOrphanedBrowsers;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrphanedBrowserSweepTest {
	private static final Path PLANT_ROOT = Paths.get("build/tmp/orphanedBrowserSweep");
	private static final String LIFETIME_IN_SECONDS = "300";

	private final List<ProcessHandle> planted = new ArrayList<>();

	@AfterEach
	void killWhateverSurvivedTheCase() {
		planted.forEach(ProcessHandle::destroyForcibly);
	}

	@Test
	void killsABrowserProcessWhoseTestRunIsGone() throws Exception {
		ProcessHandle orphan = plantADetachedFakeDriver("orphan");

		List<String> swept = sweepOrphanedBrowsers();

		assertThat(swept).anyMatch(entry -> entry.contains(String.valueOf(orphan.pid())));
		assertThat(hasExitedWithinTheGuard(orphan)).isTrue();
	}

	@Test
	void leavesTheBrowserOfARunningTestRunAlone() throws Exception {
		ProcessHandle inUse = plantAFakeDriverUnderThisJvm("inUse");

		List<String> swept = sweepOrphanedBrowsers();

		assertThat(swept).noneMatch(entry -> entry.contains(String.valueOf(inUse.pid())));
		assertThat(inUse.isAlive()).isTrue();
	}

	@Test
	void failsTheRunWhenTheSweepFoundALeftover() {
		assertThatThrownBy(() -> failOnAnyLeftover(List.of("geckodriver [pid 4711]"))).isInstanceOf(IllegalStateException.class).hasMessageContaining("4711");
	}

	@Test
	void staysSilentWhenNothingWasLeftBehind() {
		assertThatCode(() -> failOnAnyLeftover(List.of())).doesNotThrowAnyException();
	}

	private ProcessHandle plantAFakeDriverUnderThisJvm(String plantName) throws Exception {
		Path driver = fakeDriverNamedLikeTheRealOne(plantName);
		ProcessHandle plant = new ProcessBuilder(driver.toString(), LIFETIME_IN_SECONDS).start().toHandle();
		planted.add(plant);
		return plant;
	}

	private ProcessHandle plantADetachedFakeDriver(String plantName) throws Exception {
		Path driver = fakeDriverNamedLikeTheRealOne(plantName);
		new ProcessBuilder("setsid", "--fork", driver.toString(), LIFETIME_IN_SECONDS).start().waitFor();
		ProcessHandle plant = awaitTheProcessRunning(driver);
		planted.add(plant);
		return plant;
	}

	private static Path fakeDriverNamedLikeTheRealOne(String plantName) throws IOException {
		Path driver = PLANT_ROOT.resolve(plantName).resolve("geckodriver").toAbsolutePath();
		Files.createDirectories(driver.getParent());
		Files.copy(Paths.get("/bin/sleep"), driver, REPLACE_EXISTING);
		driver.toFile().setExecutable(true);
		return driver;
	}

	private static ProcessHandle awaitTheProcessRunning(Path driver) throws InterruptedException {
		for (int attempt = 0; attempt < 100; attempt++) {
			Optional<ProcessHandle> started = ProcessHandle.allProcesses().filter(process -> runs(process, driver)).findFirst();
			if (started.isPresent()) {
				return started.get();
			}
			MILLISECONDS.sleep(50);
		}
		throw new IllegalStateException("the planted " + driver + " never appeared among the running processes");
	}

	private static boolean runs(ProcessHandle process, Path driver) {
		return process.info().command().filter(driver.toString()::equals).isPresent();
	}

	private static boolean hasExitedWithinTheGuard(ProcessHandle process) throws InterruptedException {
		for (int attempt = 0; attempt < 100 && process.isAlive(); attempt++) {
			MILLISECONDS.sleep(50);
		}
		return !process.isAlive();
	}
}
