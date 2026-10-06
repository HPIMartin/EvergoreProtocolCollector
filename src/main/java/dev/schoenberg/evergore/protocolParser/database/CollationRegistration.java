package dev.schoenberg.evergore.protocolParser.database;

import java.sql.Connection;
import java.sql.SQLException;

interface CollationRegistration {
	GermanOrderCollation registerOn(Connection connection) throws SQLException;
}
