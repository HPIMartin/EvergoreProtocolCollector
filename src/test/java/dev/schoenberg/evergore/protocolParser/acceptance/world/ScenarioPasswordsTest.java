package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioPasswordsTest {
	private final ScenarioPasswords tested = new ScenarioPasswords();

	@Test
	void answersThePasswordIssuedToAnAvatar() {
		tested.record("Aurora", "Issued-1!");

		Optional<String> issued = tested.issuedTo("Aurora");

		assertThat(issued).contains("Issued-1!");
	}

	@Test
	void answersTheLatestPasswordWhenAnAvatarWasIssuedAnother() {
		tested.record("Aurora", "Issued-1!");
		tested.record("Aurora", "Issued-2!");

		Optional<String> issued = tested.issuedTo("Aurora");

		assertThat(issued).contains("Issued-2!");
	}

	@Test
	void answersTheFirstPasswordWhenAnAvatarWasIssuedAnother() {
		tested.record("Aurora", "Issued-1!");
		tested.record("Boreas", "Issued-3!");
		tested.record("Aurora", "Issued-2!");

		Optional<String> issued = tested.firstIssuedTo("Aurora");

		assertThat(issued).contains("Issued-1!");
	}

	@Test
	void answersNoFirstPasswordForAnAvatarNoPasswordWasIssuedTo() {
		tested.record("Aurora", "Issued-1!");

		Optional<String> issued = tested.firstIssuedTo("Boreas");

		assertThat(issued).isEmpty();
	}

	@Test
	void answersNothingForAnAvatarNoPasswordWasIssuedTo() {
		tested.record("Aurora", "Issued-1!");

		Optional<String> issued = tested.issuedTo("Boreas");

		assertThat(issued).isEmpty();
	}
}
