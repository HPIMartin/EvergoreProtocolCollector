package dev.schoenberg.evergore.protocolParser.helper.fileLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import dev.schoenberg.evergore.protocolParser.helper.selenium.FileLoader;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.notExists;
import static java.nio.file.Paths.get;

public class ResourceFileLoader implements FileLoader {
	private Path tempFolder;

	@Override
	public File fetchFile(String path) {
		initTempFolder();
		return fetchFileFromResources(path);
	}

	private void initTempFolder() {
		if (tempFolder == null) {
			tempFolder = silentThrow(() -> createTempDirectory("evergore"));
			tempFolder.toFile().deleteOnExit();
		}
	}

	private File fetchFileFromResources(String path) {
		Path filePath = get(tempFolder.toString(), path);
		if (notExists(filePath)) {
			silentThrow(() -> {
				InputStream resource = load(path);
				createParentDirs(filePath);
				return copy(resource, filePath);
			});
			filePath.toFile().deleteOnExit();
		}
		return filePath.toFile();
	}

	private void createParentDirs(Path filePath) throws IOException {
		Path parent = filePath.getParent();
		createDirectories(parent);

		while (!tempFolder.toString().equals(parent.toString())) {
			parent.toFile().deleteOnExit();
			parent = parent.getParent();
		}
	}

	private InputStream load(String resourcePath) {
		InputStream resource = this.getClass().getClassLoader().getResourceAsStream(resourcePath);

		if (resource == null) {
			throw new RuntimeException("Resource not found");
		}

		return resource;
	}
}
