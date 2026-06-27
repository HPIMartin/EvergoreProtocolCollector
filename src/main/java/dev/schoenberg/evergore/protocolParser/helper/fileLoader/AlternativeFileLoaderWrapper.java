package dev.schoenberg.evergore.protocolParser.helper.fileLoader;

import java.io.File;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.selenium.FileLoader;

public class AlternativeFileLoaderWrapper implements FileLoader {

	private final FileLoader primaryLoader;
	private final FileLoader backupLoader;
	private final Logger logger;

	public AlternativeFileLoaderWrapper(FileLoader primaryLoader, FileLoader backupLoader, Logger logger) {
		this.primaryLoader = primaryLoader;
		this.backupLoader = backupLoader;
		this.logger = logger;
	}

	@Override
	public File fetchFile(String path) {
		try {
			File result = primaryLoader.fetchFile(path);
			if (result != null) {
				logger.info("Found file in primary source.");
				return result;
			}
		} catch (Exception e) {}

		logger.info("Trying to fetch file from backup...");
		return backupLoader.fetchFile(path);
	}
}
