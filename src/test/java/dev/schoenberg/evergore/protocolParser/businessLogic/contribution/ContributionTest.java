package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ContributionTest {
	@ParameterizedTest(name = "{0}")
	@CsvSource({"Alessia, 57938, 0, 45120, 200608, -97550", "Bambor, 58410, 0, 169254, 226494, 1170", "Evildead, 45217, 0, 292118, 239370, 97965",
			"Fugger, 0, 247053, 1171710, 1978211, -1053554", "Aargh, 0, 0, 44208, 44208, 0"})
	void answersTheGuildValueTheSheetWasVerifiedAgainst(String avatar, long bankDeposited, long bankWithdrawn, double storageDeposited, double storageWithdrawn, double expected) {
		Contribution tested = new Contribution(bankDeposited, bankWithdrawn, storageDeposited, storageWithdrawn, Optional.of(GuildShare.NOTHING));

		assertThat(tested.net()).isCloseTo(expected, within(1e-6));
	}

	@Test
	void addsUpTheContributionsOfEveryAvatarIntoTheGuildsOwn() {
		List<Contribution> perAvatar = List
				.of(new Contribution(1500, 200, 185.04, 300.0, Optional.of(GuildShare.NOTHING)), new Contribution(750, 0, 46.26, 0.0, Optional.of(GuildShare.NOTHING)));

		Contribution total = Contribution.sumOf(perAvatar);

		assertThat(total.bankDeposited()).isEqualTo(2250);
		assertThat(total.bankWithdrawn()).isEqualTo(200);
		assertThat(total.storageDeposited()).isCloseTo(231.30, within(1e-6));
		assertThat(total.storageWithdrawn()).isCloseTo(300.0, within(1e-6));
	}

	@Test
	void totalsNothingWhileTheGuildHasNoAvatarAtAll() {
		assertThat(Contribution.sumOf(List.of())).isEqualTo(Contribution.NOTHING);
	}

	@Test
	void roundsEachStorageSumToWholeGoldAndLeavesTheBankSumsAlone() {
		Contribution tested = new Contribution(1500, 200, 185.04, 300.5, Optional.of(GuildShare.NOTHING)).inWholeGold();

		assertThat(tested).isEqualTo(new Contribution(1500, 200, 185, 301, Optional.of(GuildShare.NOTHING)));
	}

	@Test
	void answersAWholeNetOnceTheStorageSumsAreWholeGold() {
		Contribution tested = new Contribution(1500, 200, 185.04, 300.5, Optional.of(GuildShare.NOTHING)).inWholeGold();

		assertThat(tested.net()).isEqualTo(1184);
	}

	@Test
	void totalsTheRoundedContributionsRatherThanRoundingTheirTrueSum() {
		List<Contribution> rounded = List
				.of(new Contribution(0, 0, 100.4, 0, Optional.of(GuildShare.NOTHING)).inWholeGold(),
						new Contribution(0, 0, 100.4, 0, Optional.of(GuildShare.NOTHING)).inWholeGold(),
						new Contribution(0, 0, 100.4, 0, Optional.of(GuildShare.NOTHING)).inWholeGold());

		Contribution total = Contribution.sumOf(rounded);

		assertThat(total.storageDeposited()).isEqualTo(300);
	}

	@Test
	void answersNoGuildShareWhileNoRecomputeHasProducedOne() {
		Contribution tested = new Contribution(1500, 200, 185.04, 300.0, Optional.empty());

		assertThat(tested.guildShare()).isEmpty();
	}

	@Test
	void roundsEachFlowOfTheGuildShareOnItsOwn() {
		Contribution tested = new Contribution(0, 0, 100.4, 0, Optional.of(new GuildShare(0.5, 0.4))).inWholeGold();

		assertThat(tested.guildShare()).contains(new GuildShare(1, 0));
	}

	@Test
	void totalsNoGuildShareOnceOneAvatarIsMissingHisOwn() {
		List<Contribution> perAvatar = List.of(new Contribution(0, 0, 10, 0, Optional.of(new GuildShare(20, 0))), new Contribution(0, 0, 10, 0, Optional.empty()));

		Contribution total = Contribution.sumOf(perAvatar);

		assertThat(total.guildShare()).isEmpty();
	}

	@Test
	void totalsAGuildShareOfNothingForAGuildWithoutAnyAvatarRatherThanAnUnknownOne() {
		Contribution total = Contribution.sumOf(List.of());

		assertThat(total.guildShare()).contains(GuildShare.NOTHING);
	}

	@Test
	void tellsAnEmptyGuildApartFromOneWhoseSingleAvatarWasNeverRecomputed() {
		Contribution lonelyAndUnrecomputed = Contribution.sumOf(List.of(new Contribution(0, 0, 0, 0, Optional.empty())));

		assertThat(lonelyAndUnrecomputed.guildShare()).isEmpty();
	}

	@Test
	void totalsBothFlowsOfEveryAvatarWhileAllOfThemAreKnown() {
		List<Contribution> perAvatar = List
				.of(new Contribution(0, 0, 10, 0, Optional.of(new GuildShare(20, 3))), new Contribution(0, 0, 10, 0, Optional.of(new GuildShare(30, 4))));

		Contribution total = Contribution.sumOf(perAvatar);

		assertThat(total.guildShare()).contains(new GuildShare(50, 7));
	}

	@Test
	void totalsWholeGoldToTheExactSumOfTheRoundedContributions() {
		List<Contribution> rounded = List
				.of(new Contribution(1500, 200, 185.04, 300.0, Optional.of(GuildShare.NOTHING)).inWholeGold(),
						new Contribution(750, 0, 46.26, 0.0, Optional.of(GuildShare.NOTHING)).inWholeGold());

		Contribution total = Contribution.sumOf(rounded);

		assertThat(total.storageDeposited()).isEqualTo(231);
		assertThat(total.net()).isEqualTo(1981);
	}
}
