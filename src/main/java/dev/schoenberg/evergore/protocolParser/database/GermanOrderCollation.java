package dev.schoenberg.evergore.protocolParser.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.Collator;

import org.sqlite.Collation;

import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;

final class GermanOrderCollation extends Collation {
	static final String NAME = "GERMAN_ORDER";

	private final Collator collator;

	private GermanOrderCollation(Collator collator) {
		this.collator = collator;
	}

	static GermanOrderCollation forOneConnection() {
		return new GermanOrderCollation(GermanOrder.ownCollator());
	}

	static GermanOrderCollation registerOn(Connection connection) throws SQLException {
		GermanOrderCollation collation = forOneConnection();
		Collation.create(connection, NAME, collation);
		return collation;
	}

	Collator collator() {
		return collator;
	}

	@Override
	protected int xCompare(String left, String right) {
		return collator.compare(left, right);
	}
}
