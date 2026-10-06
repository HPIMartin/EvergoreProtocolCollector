package dev.schoenberg.evergore.protocolParser.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.Collator;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.database.ThreadStates.SETTLING_BOUND;
import static dev.schoenberg.evergore.protocolParser.database.ThreadStates.settled;
import static java.lang.Thread.State.BLOCKED;
import static java.lang.Thread.State.TERMINATED;
import static org.assertj.core.api.Assertions.assertThat;

class GermanOrderCollationTest {
	private static final String IN_MEMORY = "jdbc:sqlite::memory:";

	@Test
	void everyConnectionsCollationHasACollatorOfItsOwn() {
		Collator first = GermanOrderCollation.forOneConnection().collator();

		Collator second = GermanOrderCollation.forOneConnection().collator();

		assertThat(first).isNotSameAs(second);
	}

	@Test
	void everyRegistrationGetsACollationOfItsOwn() throws SQLException {
		try (Connection one = DriverManager.getConnection(IN_MEMORY); Connection other = DriverManager.getConnection(IN_MEMORY)) {
			GermanOrderCollation first = GermanOrderCollation.registerOn(one);

			GermanOrderCollation second = GermanOrderCollation.registerOn(other);

			assertThat(first).isNotSameAs(second);
			assertThat(first.collator()).isNotSameAs(second.collator());
		}
	}

	@Test
	void aCollationComparesUnderItsOwnCollator() throws InterruptedException {
		GermanOrderCollation collation = GermanOrderCollation.forOneConnection();
		Thread comparing = comparingOn(collation);

		Thread.State whileItsCollatorIsHeld;
		synchronized (collation.collator()) {
			comparing.start();
			whileItsCollatorIsHeld = settled(comparing);
		}
		comparing.join();

		assertThat(whileItsCollatorIsHeld).isEqualTo(BLOCKED);
	}

	@Test
	void aCollationIsNotHeldUpWhileAnotherConnectionsCollationWaits() throws InterruptedException {
		GermanOrderCollation waiting = GermanOrderCollation.forOneConnection();
		Thread held = comparingOn(waiting);
		Thread free = comparingOn(GermanOrderCollation.forOneConnection());

		Thread.State otherWhileOneIsHeld;
		synchronized (waiting.collator()) {
			held.start();
			settled(held);
			free.start();
			free.join(SETTLING_BOUND);
			otherWhileOneIsHeld = free.getState();
		}
		held.join();
		free.join();

		assertThat(otherWhileOneIsHeld).isEqualTo(TERMINATED);
	}

	private static Thread comparingOn(GermanOrderCollation collation) {
		return new Thread(() -> collation.xCompare("Äpfel", "Zwiebel"));
	}
}
