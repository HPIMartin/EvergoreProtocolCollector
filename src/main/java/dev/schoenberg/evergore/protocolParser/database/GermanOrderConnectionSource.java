package dev.schoenberg.evergore.protocolParser.database;

import java.sql.SQLException;

import com.j256.ormlite.jdbc.JdbcDatabaseConnection;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.support.DatabaseConnection;

final class GermanOrderConnectionSource extends JdbcPooledConnectionSource {
	private final CollationRegistration registration;

	GermanOrderConnectionSource(String url) throws SQLException {
		this(url, GermanOrderCollation::registerOn);
	}

	GermanOrderConnectionSource(String url, CollationRegistration registration) throws SQLException {
		super(url);
		this.registration = registration;
	}

	@Override
	protected DatabaseConnection makeConnection(Logger logger) throws SQLException {
		DatabaseConnection connection = super.makeConnection(logger);
		try {
			registration.registerOn(((JdbcDatabaseConnection) connection).getUnderlyingConnection());
		} catch (SQLException | RuntimeException refused) {
			connection.closeQuietly();
			throw refused;
		}
		return connection;
	}

	@Override
	public void close() throws SQLException {
		try {
			super.close();
		} catch (SQLException | RuntimeException failure) {
			throw failure;
		} catch (Exception failure) {
			throw new SQLException(failure);
		}
	}
}
