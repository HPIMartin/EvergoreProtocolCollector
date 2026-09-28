package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class ScenarioDatabase {
	private static final Path FOLDER = Paths.get("build/tmp/acceptance");
	private static final String[] SIDECARS = {"", "-journal", "-wal", "-shm"};

	private final Path file = FOLDER.resolve(UUID.randomUUID() + ".sqlite");

	public void prepare() {
		silentThrow(() -> Files.createDirectories(FOLDER));
	}

	public Path file() {
		return file;
	}

	public void delete() {
		for (String sidecar : SIDECARS) {
			silentThrow(() -> Files.deleteIfExists(Paths.get(file + sidecar)));
		}
	}
}
