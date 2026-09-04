package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.KEY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.TABLE;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public record StoredMetaSums(Map<String, String> values) {
	public StoredMetaSums {
		values = Map.copyOf(values);
	}

	public static StoredMetaSums readFrom(Path database) {
		return silentThrow(() -> {
			Map<String, String> values = new LinkedHashMap<>();
			try (Connection connection = DriverManager.getConnection(readOnlyUrlOf(database)); Statement statement = connection.createStatement()) {
				try (ResultSet rows = statement.executeQuery("SELECT \"" + KEY_COLUMN + "\", value FROM " + TABLE)) {
					while (rows.next()) {
						String value = rows.getString("value");
						if (value != null) {
							values.put(rows.getString(KEY_COLUMN), value);
						}
					}
				}
			}
			return new StoredMetaSums(values);
		});
	}

	private static String readOnlyUrlOf(Path database) {
		return "jdbc:sqlite:file:" + database.toAbsolutePath() + "?mode=ro";
	}
}
