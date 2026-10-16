package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class GeckoDriverExecutableTest {
	private static final int PARALLEL_STARTS = 8;
	private static final String PINNED_DRIVER_PROPERTY = "webdriver.gecko.driver";

	@TempDir
	Path drivers;

	private final AtomicInteger lookUps = new AtomicInteger();
	private final CountDownLatch everyStartHasArrived = new CountDownLatch(PARALLEL_STARTS);
	private File driver;

	@BeforeEach
	void placeAnExecutableDriver() throws IOException {
		driver = executableIn(drivers.resolve("geckodriver"));
	}

	@Test
	void parallelStartsLookTheDriverUpOnce() throws Exception {
		GeckoDriverExecutable executable = new GeckoDriverExecutable(() -> {
			lookUps.incrementAndGet();
			awaitEveryStart();
			return driver;
		});

		List<File> resolved = resolveAtOnce(executable);

		assertThat(lookUps).hasValue(1);
		assertThat(resolved).hasSize(PARALLEL_STARTS).containsOnly(driver);
	}

	@Test
	void aLookUpThatFailedIsTriedAgainOnTheNextStart() {
		GeckoDriverExecutable executable = new GeckoDriverExecutable(() -> {
			if (lookUps.incrementAndGet() == 1) {
				throw new IllegalStateException("Selenium Manager answered no driver");
			}
			return driver;
		});
		Throwable firstStart = catchThrowable(executable::file);

		File resolved = executable.file();

		assertThat(firstStart).isInstanceOf(IllegalStateException.class);
		assertThat(resolved).isEqualTo(driver);
		assertThat(lookUps).hasValue(2);
	}

	@Test
	void aLookUpThatAnsweredNoExecutableIsTriedAgainOnTheNextStart() {
		GeckoDriverExecutable executable = new GeckoDriverExecutable(() -> lookUps.incrementAndGet() == 1 ? new File("") : driver);
		Throwable firstStart = catchThrowable(executable::file);

		File resolved = executable.file();

		assertThat(firstStart).isInstanceOf(IllegalStateException.class);
		assertThat(resolved).isEqualTo(driver);
		assertThat(lookUps).hasValue(2);
	}

	@Test
	void aLookUpThatAnsweredANonExecutableFileIsTriedAgainOnTheNextStart() throws IOException {
		File plain = Files.createFile(drivers.resolve("plain-geckodriver")).toFile();
		GeckoDriverExecutable executable = new GeckoDriverExecutable(() -> lookUps.incrementAndGet() == 1 ? plain : driver);
		Throwable firstStart = catchThrowable(executable::file);

		File resolved = executable.file();

		assertThat(firstStart).isInstanceOf(IllegalStateException.class);
		assertThat(resolved).isEqualTo(driver);
	}

	@Test
	void aKeptDriverThatLostItsExecutableBitIsLookedUpAgain() throws IOException {
		File replacement = executableIn(drivers.resolve("replacement-geckodriver"));
		GeckoDriverExecutable executable = new GeckoDriverExecutable(() -> lookUps.incrementAndGet() == 1 ? driver : replacement);
		executable.file();
		driver.setExecutable(false);

		File resolved = executable.file();

		assertThat(resolved).isEqualTo(replacement);
	}

	@Test
	void aDriverThatVanishedIsLookedUpAgain() throws IOException {
		File replacement = executableIn(drivers.resolve("replacement-geckodriver"));
		GeckoDriverExecutable executable = new GeckoDriverExecutable(() -> lookUps.incrementAndGet() == 1 ? driver : replacement);
		executable.file();
		Files.delete(driver.toPath());

		File resolved = executable.file();

		assertThat(resolved).isEqualTo(replacement);
	}

	@Test
	void aDriverPinnedByItsSystemPropertyIsTakenBeforeTheOneOnThePath() throws IOException {
		File pinned = executableIn(drivers.resolve("pinned-geckodriver"));
		String before = System.setProperty(PINNED_DRIVER_PROPERTY, pinned.getPath());
		try {
			File resolved = GeckoDriverExecutable.pinnedOr(driver.getPath(), this::lookUp);

			assertThat(resolved).isEqualTo(pinned);
			assertThat(lookUps).hasValue(0);
		} finally {
			restore(before);
		}
	}

	@Test
	void aDriverOnThePathIsTakenWithoutALookUp() {
		String before = System.clearProperty(PINNED_DRIVER_PROPERTY);
		try {
			File resolved = GeckoDriverExecutable.pinnedOr(driver.getPath(), this::lookUp);

			assertThat(resolved).isEqualTo(driver);
			assertThat(lookUps).hasValue(0);
		} finally {
			restore(before);
		}
	}

	@Test
	void withNeitherPinNorPathTheDriverIsLookedUp() {
		String before = System.clearProperty(PINNED_DRIVER_PROPERTY);
		try {
			File resolved = GeckoDriverExecutable.pinnedOr(null, this::lookUp);

			assertThat(resolved).isEqualTo(driver);
			assertThat(lookUps).hasValue(1);
		} finally {
			restore(before);
		}
	}

	private File lookUp() {
		lookUps.incrementAndGet();
		return driver;
	}

	private static void restore(String before) {
		if (before == null) {
			System.clearProperty(PINNED_DRIVER_PROPERTY);
		} else {
			System.setProperty(PINNED_DRIVER_PROPERTY, before);
		}
	}

	private void awaitEveryStart() {
		try {
			if (!everyStartHasArrived.await(5, SECONDS)) {
				throw new IllegalStateException("not every start reached the look-up within five seconds");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private List<File> resolveAtOnce(GeckoDriverExecutable executable) throws Exception {
		CountDownLatch startingGun = new CountDownLatch(1);
		Callable<File> start = () -> {
			startingGun.await();
			everyStartHasArrived.countDown();
			return executable.file();
		};
		ExecutorService starts = Executors.newFixedThreadPool(PARALLEL_STARTS);
		try {
			List<Future<File>> running = IntStream.range(0, PARALLEL_STARTS).mapToObj(each -> starts.submit(start)).toList();
			startingGun.countDown();
			return running.stream().map(GeckoDriverExecutableTest::resultOf).toList();
		} finally {
			starts.shutdownNow();
		}
	}

	private static File resultOf(Future<File> start) {
		try {
			return start.get();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static File executableIn(Path path) throws IOException {
		File file = Files.createFile(path).toFile();
		file.setExecutable(true);
		return file;
	}
}
