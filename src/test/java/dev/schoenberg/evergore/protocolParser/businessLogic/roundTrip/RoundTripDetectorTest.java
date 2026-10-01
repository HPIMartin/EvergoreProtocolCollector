package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.ACHAT_ARMBRUST;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.BOLZEN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.BUCHENHOLZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.EISENBARREN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.FEDERN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.HARZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.JAGDPFEILE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KRISTALLAT;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KUPFERERZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MAGIEESSENZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.PFEILE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.STEINKOHLE;
import static org.assertj.core.api.Assertions.assertThat;

class RoundTripDetectorTest {
	private static final Instant T = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void reportsATraderGoodWithdrawnAndDepositedAgainWithinTheWindow() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 100, T.plusSeconds(3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void reportsOnlyTheOverlappingQuantityWhenTheDepositIsSmaller() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 60, T.plusSeconds(3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 60));
	}

	@Test
	void reportsOnlyTheOverlappingQuantityWhenTheWithdrawalIsSmaller() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 50, T), deposit("Alrik", FEDERN, 80, T.plusSeconds(3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 50));
	}

	@Test
	void reportsADepositExactlyFortyEightHoursAfterTheWithdrawal() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 100, T.plus(RoundTripDetector.WINDOW)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void leavesADepositAloneOnceTheWindowHasPassed() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 100, T.plus(RoundTripDetector.WINDOW).plusSeconds(60)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).isEmpty();
	}

	@Test
	void matchesADepositAgainstTheOldestOpenLotThatIsStillInsideTheWindow() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", FEDERN, 100, T), withdrawal("Alrik", FEDERN, 100, T.plusSeconds(47 * 3600)), deposit("Alrik", FEDERN, 100, T.plusSeconds(49 * 3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void keepsItemsApart() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", HARZ, 100, T.plusSeconds(3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).isEmpty();
	}

	@Test
	void keepsAvatarsApart() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Brynn", FEDERN, 100, T.plusSeconds(3600)));

		RoundTripReport alriksReport = RoundTripDetector.detect("Alrik", entries);
		RoundTripReport brynnsReport = RoundTripDetector.detect("Brynn", entries);

		assertThat(alriksReport.roundTrips()).isEmpty();
		assertThat(brynnsReport.roundTrips()).isEmpty();
	}

	@Test
	void countsASameMinuteWithdrawalAndDepositAsARoundTripWhateverTheirOrderInTheList() {
		List<ResolvedStorageEntry> entries = List.of(deposit("Alrik", FEDERN, 100, T), withdrawal("Alrik", FEDERN, 100, T));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void ignoresAGoodOutsideTheTraderTier() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", KUPFERERZ, 100, T), deposit("Alrik", KUPFERERZ, 100, T.plusSeconds(3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).isEmpty();
	}

	@Test
	void reportsTheAmmunitionTheGamesTraderSellsWithdrawnAndDepositedAgain() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", PFEILE, 100, T), withdrawal("Alrik", BOLZEN, 100, T), withdrawal("Alrik", MAGIEESSENZ, 100, T),
						deposit("Alrik", PFEILE, 100, T.plusSeconds(3600)), deposit("Alrik", BOLZEN, 100, T.plusSeconds(3600)),
						deposit("Alrik", MAGIEESSENZ, 100, T.plusSeconds(3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips())
				.containsExactlyInAnyOrder(new RoundTrip("Alrik", PFEILE, 100), new RoundTrip("Alrik", BOLZEN, 100), new RoundTrip("Alrik", MAGIEESSENZ, 100));
	}

	@Test
	void ignoresAmmunitionThatCanOnlyBeCrafted() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", JAGDPFEILE, 100, T), deposit("Alrik", JAGDPFEILE, 100, T.plusSeconds(3600)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).isEmpty();
	}

	@Test
	void aDepositOfAmmunitionTheGamesTraderSellsAnswersItsOwnWithdrawalBeforeItCountsAsCrafted() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", PFEILE, 135, T), withdrawal("Alrik", FEDERN, 5, T), deposit("Alrik", PFEILE, 135, T.plusSeconds(3600)),
						deposit("Alrik", FEDERN, 5, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactlyInAnyOrder(new RoundTrip("Alrik", PFEILE, 135), new RoundTrip("Alrik", FEDERN, 5));
	}

	@Test
	void onlyTheAmmunitionBeyondItsOwnOpenWithdrawalCountsAsCraftedAndUsesUpItsRecipe() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", PFEILE, 135, T), withdrawal("Alrik", FEDERN, 10, T), deposit("Alrik", PFEILE, 270, T.plusSeconds(3600)),
						deposit("Alrik", FEDERN, 10, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactlyInAnyOrder(new RoundTrip("Alrik", PFEILE, 135), new RoundTrip("Alrik", FEDERN, 5));
	}

	@Test
	void abstainsFromAnOpenWithdrawalOfAmmunitionTheGamesTraderSellsWhenAnUnreadRecipeDepositArrives() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", PFEILE, 100, T), deposit("Alrik", ACHAT_ARMBRUST, 1, T.plusSeconds(3600)), deposit("Alrik", PFEILE, 100, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).isEmpty();
		assertThat(report.abstentions()).containsExactly(new RoundTripAbstention("Alrik", PFEILE));
	}

	@Test
	void chargesOnlyTheRestockedShareAfterACrafterBuysBackWhatTheRecipeAlreadyConsumed() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", FEDERN, 10, T), deposit("Alrik", PFEILE, 135, T.plusSeconds(3600)), deposit("Alrik", FEDERN, 10, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 5));
	}

	@Test
	void consumesNoMoreForAProductDepositedInTwoLinesThanForTheSameQuantityDepositedAtOnce() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", STEINKOHLE, 2, T), deposit("Alrik", EISENBARREN, 1, T.plusSeconds(1800)), deposit("Alrik", EISENBARREN, 1, T.plusSeconds(3600)),
						deposit("Alrik", STEINKOHLE, 2, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", STEINKOHLE, 1));
	}

	@Test
	void aProductTheGameDoesNotCraftBetweenTheTwoMovesConsumesNothing() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", KUPFERERZ, 10, T.plusSeconds(3600)), deposit("Alrik", FEDERN, 100, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void staysSilentForACrafterWhoWithdrawsMaterialDepositsTheProductAndRestocksTheMaterial() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", BUCHENHOLZ, 6, T), withdrawal("Alrik", FEDERN, 5, T), deposit("Alrik", PFEILE, 135, T.plusSeconds(3600)),
						deposit("Alrik", BUCHENHOLZ, 6, T.plusSeconds(7200)), deposit("Alrik", FEDERN, 5, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).isEmpty();
	}

	@Test
	void abstainsFromAPairWhoseOpenWithdrawalMeetsAnUnreadRecipeDeposit() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", KRISTALLAT, 100, T), deposit("Alrik", ACHAT_ARMBRUST, 1, T.plusSeconds(3600)), deposit("Alrik", KRISTALLAT, 100, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).isEmpty();
		assertThat(report.abstentions()).containsExactly(new RoundTripAbstention("Alrik", KRISTALLAT));
	}

	@Test
	void reportsTheRoundTripWhenAnUnreadRecipeDepositMeetsNoOpenWithdrawal() {
		List<ResolvedStorageEntry> entries = List
				.of(deposit("Alrik", ACHAT_ARMBRUST, 1, T), withdrawal("Alrik", KRISTALLAT, 100, T.plusSeconds(3600)), deposit("Alrik", KRISTALLAT, 100, T.plusSeconds(7200)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", KRISTALLAT, 100));
		assertThat(report.abstentions()).isEmpty();
	}

	@Test
	void abstainsOnlyThePairTheUnreadDepositTouches() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", KRISTALLAT, 100, T), deposit("Alrik", ACHAT_ARMBRUST, 1, T.plusSeconds(3600)), deposit("Alrik", KRISTALLAT, 100, T.plusSeconds(7200)),
						withdrawal("Alrik", FEDERN, 50, T.plusSeconds(10800)), deposit("Alrik", FEDERN, 50, T.plusSeconds(14400)));

		RoundTripReport report = RoundTripDetector.detect("Alrik", entries);

		assertThat(report.roundTrips()).containsExactly(new RoundTrip("Alrik", FEDERN, 50));
		assertThat(report.abstentions()).containsExactly(new RoundTripAbstention("Alrik", KRISTALLAT));
	}

	private static ResolvedStorageEntry withdrawal(String avatar, EvergoreItem item, int quantity, Instant at) {
		return new ResolvedStorageEntry(new StorageEntry(at, avatar, quantity, item.ingameName, 100, TransferType.ENTNAHME), item);
	}

	private static ResolvedStorageEntry deposit(String avatar, EvergoreItem item, int quantity, Instant at) {
		return new ResolvedStorageEntry(new StorageEntry(at, avatar, quantity, item.ingameName, 100, TransferType.EINLAGERUNG), item);
	}
}
