package dev.schoenberg.evergore.protocolParser.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedAtKey;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry.KEY_COLUMN;

final class LastUpdatedInstantFill {
	private static final MetaInformationKey<LocalDateTime> BERLIN_MINUTE = getLastUpdatedKey();
	private static final MetaInformationKey<Instant> INSTANT = getLastUpdatedAtKey();
	private static final String READ = "SELECT value FROM " + MetaInformationEntry.TABLE + " WHERE " + KEY_COLUMN + " = ?";
	private static final String WRITE = "INSERT OR REPLACE INTO " + MetaInformationEntry.TABLE + " (" + KEY_COLUMN + ", value) VALUES (?, ?)";

	private final String url;
	private final Logger logger;

	LastUpdatedInstantFill(String url, Logger logger) {
		this.url = url;
		this.logger = logger;
	}

	void fill() throws SQLException {
		try (Connection connection = DriverManager.getConnection(url)) {
			Optional<String> berlinMinute = valueOf(connection, BERLIN_MINUTE);
			Optional<Instant> named = berlinMinute.flatMap(this::instantNamedBy);
			if (named.isEmpty() || valueOf(connection, INSTANT).filter(instant -> namesTheSameMinute(instant, berlinMinute.get())).isPresent()) {
				return;
			}
			try (PreparedStatement write = connection.prepareStatement(WRITE)) {
				write.setString(1, INSTANT.id);
				write.setString(2, INSTANT.serialize(named.get()));
				write.executeUpdate();
			}
		}
	}

	private Optional<Instant> instantNamedBy(String berlinMinute) {
		try {
			return Optional.of(BERLIN_MINUTE.deserialize(berlinMinute).atZone(APP_ZONE).toInstant());
		} catch (DateTimeParseException unreadable) {
			logger.warn("The stored " + BERLIN_MINUTE.id + " '" + berlinMinute + "' names no Berlin minute, so " + INSTANT.id + " is left as it is");
			return Optional.empty();
		}
	}

	private static boolean namesTheSameMinute(String storedInstant, String berlinMinute) {
		try {
			return BERLIN_MINUTE.serialize(LocalDateTime.ofInstant(INSTANT.deserialize(storedInstant), APP_ZONE)).equals(berlinMinute);
		} catch (NumberFormatException unreadable) {
			return false;
		}
	}

	private static Optional<String> valueOf(Connection connection, MetaInformationKey<?> key) throws SQLException {
		try (PreparedStatement read = connection.prepareStatement(READ)) {
			read.setString(1, key.id);
			try (ResultSet row = read.executeQuery()) {
				return row.next() ? Optional.of(row.getString(1)) : Optional.empty();
			}
		}
	}
}
