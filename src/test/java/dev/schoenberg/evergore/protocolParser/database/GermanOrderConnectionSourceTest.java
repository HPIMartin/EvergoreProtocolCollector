package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.jdbc.JdbcDatabaseConnection;
import com.j256.ormlite.support.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.database.ThreadStates.settled;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.lang.Thread.State.BLOCKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class GermanOrderConnectionSourceTest {
	private static final Path DATABASE = Paths.get("build/tmp/test/germanOrderConnectionSourceTest.sqlite");
	private static final String TABLE = "anyTable";

	private final Map<Connection, GermanOrderCollation> registered = new HashMap<>();
	private GermanOrderConnectionSource tested;
	private GermanOrderConnectionSource testedRecording;
	private GermanOrderConnectionSource testedRefusing;

	@BeforeEach
	void openAPoolOnAFreshFile() throws SQLException {
		silentThrow(() -> Files.createDirectories(DATABASE.getParent()));
		silentThrow(() -> Files.deleteIfExists(DATABASE));
		tested = new GermanOrderConnectionSource("jdbc:sqlite:" + DATABASE);
		testedRecording = new GermanOrderConnectionSource("jdbc:sqlite:" + DATABASE, this::registerAndRecordOn);
	}

	@AfterEach
	void closeThePool() {
		tested.closeQuietly();
		testedRecording.closeQuietly();
		if (testedRefusing != null) {
			testedRefusing.closeQuietly();
		}
	}

	@Test
	void everyConnectionHeldAtTheSameTimeSortsInGermanOrder() throws SQLException {
		DatabaseConnection first = tested.getReadWriteConnection(TABLE);
		DatabaseConnection second = tested.getReadWriteConnection(TABLE);

		boolean firstSortsGerman = sortsStahlbarrenFirst(first);
		boolean secondSortsGerman = sortsStahlbarrenFirst(second);

		assertThat(second).isNotSameAs(first);
		assertThat(firstSortsGerman).isTrue();
		assertThat(secondSortsGerman).isTrue();
	}

	@Test
	void everyConnectionHeldAtTheSameTimeHasACollationOfItsOwn() throws SQLException {
		DatabaseConnection first = testedRecording.getReadWriteConnection(TABLE);

		DatabaseConnection second = testedRecording.getReadWriteConnection(TABLE);

		assertThat(collationOf(first)).isNotSameAs(collationOf(second));
		assertThat(collationOf(first).collator()).isNotSameAs(collationOf(second).collator());
	}

	@Test
	void aConnectionSortsUnderTheCollationRegisteredOnIt() throws Exception {
		DatabaseConnection connection = testedRecording.getReadWriteConnection(TABLE);
		Thread sorting = new Thread(() -> silentThrow(() -> sortsStahlbarrenFirst(connection)));

		Thread.State whileItsCollatorIsHeld;
		synchronized (collationOf(connection).collator()) {
			sorting.start();
			whileItsCollatorIsHeld = settled(sorting);
		}
		sorting.join();

		assertThat(whileItsCollatorIsHeld).isEqualTo(BLOCKED);
	}

	@Test
	void aConnectionOpenedAfterTheLastOneClosedSortsInGermanOrderToo() throws SQLException {
		tested.setMaxConnectionsFree(0);
		DatabaseConnection closedOnRelease = tested.getReadWriteConnection(TABLE);
		tested.releaseConnection(closedOnRelease);
		DatabaseConnection reopened = tested.getReadWriteConnection(TABLE);

		boolean reopenedSortsGerman = sortsStahlbarrenFirst(reopened);

		assertThat(tested.getCloseCount()).isEqualTo(1);
		assertThat(reopenedSortsGerman).isTrue();
	}

	@Test
	void aConnectionWhoseCollationCannotBeRegisteredIsClosed() throws SQLException {
		List<Connection> refused = new ArrayList<>();
		testedRefusing = new GermanOrderConnectionSource("jdbc:sqlite:" + DATABASE, connection -> {
			refused.add(connection);
			throw new SQLException("registration refused");
		});

		Throwable failure = catchThrowable(() -> testedRefusing.getReadWriteConnection(TABLE));

		assertThat(failure).hasStackTraceContaining("registration refused");
		assertThat(refused.getFirst().isClosed()).isTrue();
	}

	@Test
	void aConnectionWhoseRegistrationFailsUncheckedIsClosedToo() throws SQLException {
		List<Connection> refused = new ArrayList<>();
		testedRefusing = new GermanOrderConnectionSource("jdbc:sqlite:" + DATABASE, connection -> {
			refused.add(connection);
			throw new IllegalStateException("registration broke");
		});

		Throwable failure = catchThrowable(() -> testedRefusing.getReadWriteConnection(TABLE));

		assertThat(failure).hasMessage("registration broke");
		assertThat(refused.getFirst().isClosed()).isTrue();
	}

	@Test
	void closingTheSourceClosesTheConnectionsItKeeps() throws SQLException {
		GermanOrderConnectionSource testedClosing = new GermanOrderConnectionSource("jdbc:sqlite:" + DATABASE);
		DatabaseConnection kept = testedClosing.getReadWriteConnection(TABLE);
		testedClosing.releaseConnection(kept);

		testedClosing.close();
		boolean keptIsClosed = ((JdbcDatabaseConnection) kept).getUnderlyingConnection().isClosed();

		assertThat(keptIsClosed).isTrue();
	}

	private GermanOrderCollation registerAndRecordOn(Connection connection) throws SQLException {
		GermanOrderCollation collation = GermanOrderCollation.registerOn(connection);
		registered.put(connection, collation);
		return collation;
	}

	private GermanOrderCollation collationOf(DatabaseConnection connection) {
		return registered.get(((JdbcDatabaseConnection) connection).getUnderlyingConnection());
	}

	private static boolean sortsStahlbarrenFirst(DatabaseConnection connection) throws SQLException {
		try (Statement statement = ((JdbcDatabaseConnection) connection).getUnderlyingConnection().createStatement();
				ResultSet answer = statement.executeQuery("SELECT 'Stahlbarren' < 'Stahl-Rüstung' COLLATE " + GermanOrderCollation.NAME)) {
			return answer.next() && answer.getInt(1) == 1;
		}
	}
}
