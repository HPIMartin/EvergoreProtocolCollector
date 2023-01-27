package dev.schoenberg.evergore.protocolParser.helper.fileLoader;

import static java.nio.file.Paths.*;

import java.io.*;

import dev.schoenberg.evergore.protocolParser.helper.config.*;
import dev.schoenberg.evergore.protocolParser.helper.selenium.*;

public class DiscFileLoader implements FileLoader {
	private final Configuration config;

	public DiscFileLoader(Configuration config) {
		this.config = config;
	}

	@Override
	public File fetchFile(String path) {
		File file = get(config.evergoreFolder.toString(), path).toFile();
		if (file.exists() && file.isFile()) {
			return file;
		} else {
			throw new RuntimeException("File not found: " + path);
		}
	}
}
