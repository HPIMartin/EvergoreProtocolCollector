package dev.schoenberg.evergore.protocolParser.helper.config;

import static java.nio.file.Path.*;
import static java.nio.file.Paths.*;

import java.nio.file.*;

public class Configuration {
	private static final String ZYRTHANIA = "zyrthania";

	public final String browser = "docker";
	public final String server = ZYRTHANIA;
	public final Path evergoreFolder = get("c:", "evergore");
	public final Path credentials = of("zugang.txt");
//	public final Path credentials = of("c:", "evergore", "zugang.txt");

	public final boolean useInMemory = true;

	public final boolean initializeDatabase = true;

	public String getDatabasePath() {
		return useInMemory ? ":memory:" : "temp.sqlite";
	}
}
