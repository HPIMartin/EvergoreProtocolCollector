package dev.schoenberg.evergore.protocolParser.helper.config;

import java.nio.file.Path;

import jakarta.inject.Singleton;

import static java.nio.file.Path.of;
import static java.nio.file.Paths.get;

@Singleton
public class Configuration {

	private static final String ZYRTHANIA = "zyrthania";

	public final String browser = "docker";
	public final String server = ZYRTHANIA;
	public final Path evergoreFolder = get("c:", "evergore");
	public final Path credentials = of("zugang.txt");

	public boolean useInMemory = false;
	public String DATABASE_TEMP_SQLITE = "database/temp.sqlite";

	public String getDatabasePath() {
		return useInMemory ? ":memory:" : DATABASE_TEMP_SQLITE;
	}

	public int getCollectorInitialDelaySeconds() {
		return 30;
	}
}
