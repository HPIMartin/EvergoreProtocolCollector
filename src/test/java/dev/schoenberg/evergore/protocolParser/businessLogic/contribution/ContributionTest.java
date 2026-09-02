package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.util.List;

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
		Contribution tested = new Contribution(bankDeposited, bankWithdrawn, storageDeposited, storageWithdrawn);

		assertThat(tested.net()).isCloseTo(expected, within(1e-6));
	}

	@Test
	void addsUpTheContributionsOfEveryAvatarIntoTheGuildsOwn() {
		List<Contribution> perAvatar = List.of(new Contribution(1500, 200, 185.04, 300.0), new Contribution(750, 0, 46.26, 0.0));

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
		Contribution tested = new Contribution(1500, 200, 185.04, 300.5).inWholeGold();

		assertThat(tested).isEqualTo(new Contribution(1500, 200, 185, 301));
	}

	@Test
	void answersAWholeNetOnceTheStorageSumsAreWholeGold() {
		Contribution tested = new Contribution(1500, 200, 185.04, 300.5).inWholeGold();

		assertThat(tested.net()).isEqualTo(1184);
	}

	@Test
	void totalsTheRoundedContributionsRatherThanRoundingTheirTrueSum() {
		List<Contribution> rounded = List
				.of(new Contribution(0, 0, 100.4, 0).inWholeGold(), new Contribution(0, 0, 100.4, 0).inWholeGold(), new Contribution(0, 0, 100.4, 0).inWholeGold());

		Contribution total = Contribution.sumOf(rounded);

		assertThat(total.storageDeposited()).isEqualTo(300);
	}

	@Test
	void totalsWholeGoldToTheExactSumOfTheRoundedContributions() {
		List<Contribution> rounded = List.of(new Contribution(1500, 200, 185.04, 300.0).inWholeGold(), new Contribution(750, 0, 46.26, 0.0).inWholeGold());

		Contribution total = Contribution.sumOf(rounded);

		assertThat(total.storageDeposited()).isEqualTo(231);
		assertThat(total.net()).isEqualTo(1981);
	}
}
