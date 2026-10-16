package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.io.File;
import java.util.function.Supplier;

import org.openqa.selenium.manager.SeleniumManager;
import org.openqa.selenium.os.ExecutableFinder;

import static org.openqa.selenium.firefox.GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY;

final class GeckoDriverExecutable {
	private static final String DRIVER = "geckodriver";
	private static final GeckoDriverExecutable OF_THIS_JVM = new GeckoDriverExecutable(
			() -> pinnedOr(new ExecutableFinder().find(DRIVER), () -> new File(SeleniumManager.getInstance().getDriverPath(DRIVER))));

	private final Supplier<File> lookUp;
	private File resolved;

	GeckoDriverExecutable(Supplier<File> lookUp) {
		this.lookUp = lookUp;
	}

	static File ofThisJvm() {
		return OF_THIS_JVM.file();
	}

	static File pinnedOr(String onThePath, Supplier<File> lookUp) {
		String pinned = System.getProperty(GECKO_DRIVER_EXE_PROPERTY, onThePath);
		return pinned == null ? lookUp.get() : new File(pinned);
	}

	synchronized File file() {
		if (resolved == null || !isADriver(resolved)) {
			File answer = lookUp.get();
			if (!isADriver(answer)) {
				throw new IllegalStateException("the look-up for " + DRIVER + " answered no executable file: '" + answer + "'");
			}
			resolved = answer;
		}
		return resolved;
	}

	private static boolean isADriver(File file) {
		return file.isFile() && file.canExecute();
	}
}
