package dev.schoenberg.evergore.protocolParser.helper.config;

import static java.nio.file.Path.*;
import static java.nio.file.Paths.*;

import java.nio.file.*;

import jakarta.inject.*;

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
}
