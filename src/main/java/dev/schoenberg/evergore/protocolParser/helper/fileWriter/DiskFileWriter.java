package dev.schoenberg.evergore.protocolParser.helper.fileWriter;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.Files.*;
import static java.nio.file.Path.of;
import static java.nio.file.StandardOpenOption.*;
import static java.time.LocalDateTime.*;
import static java.time.format.DateTimeFormatter.*;

import java.nio.file.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class DiskFileWriter implements FileWriter {
	private final Configuration config;

	public DiskFileWriter(Configuration config) {
		this.config = config;
	}

	@Override
	public void write(String content, String fileName) {
		String date = ofPattern("yyyy_MM_dd HH_mm").format(now());
		Path resultFile = of(config.evergoreFolder.toString(), date + " " + fileName);
		silentThrow(() -> createDirectories(config.evergoreFolder));
		silentThrow(() -> writeString(resultFile, content, UTF_16, CREATE_NEW));
	}
}
