package dev.schoenberg.evergore.protocolParser.helper.fileLoader;

import java.io.*;

import dev.schoenberg.evergore.protocolParser.helper.selenium.*;

public class AlternativeFileLoaderWrapper implements FileLoader {

	private final FileLoader primaryLoader;
	private final FileLoader backupLoader;

	public AlternativeFileLoaderWrapper(FileLoader primaryLoader, FileLoader backupLoader) {
		this.primaryLoader = primaryLoader;
		this.backupLoader = backupLoader;
	}

	@Override
	public File fetchFile(String path) {
		try {
			File result = primaryLoader.fetchFile(path);
			if (result != null) {
				System.out.println("Found file in primary source.");
				return result;
			}
		} catch (Exception e) {}

		System.out.println("Trying to fetch file from backup...");
		return backupLoader.fetchFile(path);

	}

}
