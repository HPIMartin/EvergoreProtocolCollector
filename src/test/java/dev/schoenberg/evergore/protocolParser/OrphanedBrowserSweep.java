package dev.schoenberg.evergore.protocolParser;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static java.lang.String.join;
import static java.time.Instant.now;

public class OrphanedBrowserSweep implements BeforeAllCallback {
	private static final String DRIVER = "geckodriver";
	private static final String BROWSER = "firefox";
	private static final String JVM = "java";

	@Override
	public void beforeAll(ExtensionContext context) {
		sweep();
	}

	public static void sweep() {
		failOnAnyLeftover(sweepOrphanedBrowsers());
	}

	static List<String> sweepOrphanedBrowsers() {
		List<ProcessHandle> orphans = ProcessHandle.allProcesses().filter(OrphanedBrowserSweep::isABrowserProcess).filter(OrphanedBrowserSweep::belongsToNoRunningTest).toList();
		List<String> swept = orphans.stream().map(OrphanedBrowserSweep::describe).toList();

		orphans.forEach(ProcessHandle::destroyForcibly);

		return swept;
	}

	static void failOnAnyLeftover(List<String> swept) {
		if (swept.isEmpty()) {
			return;
		}
		throw new IllegalStateException("killed browser processes that outlived the run which started them: " + join(", ", swept)
				+ ". Each one holds around a gigabyte and that run reported nothing, so this class fails in its place.");
	}

	private static boolean isABrowserProcess(ProcessHandle process) {
		return executableOf(process).map(name -> name.equals(DRIVER) || name.startsWith(BROWSER)).orElse(false);
	}

	private static boolean belongsToNoRunningTest(ProcessHandle process) {
		for (Optional<ProcessHandle> ancestor = process.parent(); ancestor.isPresent(); ancestor = ancestor.get().parent()) {
			if (isALivingJvm(ancestor.get())) {
				return false;
			}
		}
		return true;
	}

	private static boolean isALivingJvm(ProcessHandle ancestor) {
		return ancestor.isAlive() && executableOf(ancestor).filter(JVM::equals).isPresent();
	}

	private static Optional<String> executableOf(ProcessHandle process) {
		return process.info().command().map(command -> command.substring(command.lastIndexOf('/') + 1));
	}

	private static String describe(ProcessHandle process) {
		return executableOf(process).orElse("an unnamed browser process") + " [pid " + process.pid() + ", " + ageOf(process) + " old]";
	}

	private static String ageOf(ProcessHandle process) {
		Instant startedAt = process.info().startInstant().orElse(now());
		Duration age = Duration.between(startedAt, now());
		return age.toHours() + " h " + age.toMinutesPart() + " min";
	}
}
