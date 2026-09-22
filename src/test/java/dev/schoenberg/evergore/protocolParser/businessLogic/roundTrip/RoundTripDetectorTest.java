package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.FEDERN;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.HARZ;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.KUPFERERZ;
import static org.assertj.core.api.Assertions.assertThat;

class RoundTripDetectorTest {
	private static final Instant T = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void reportsATraderGoodWithdrawnAndDepositedAgainWithinTheWindow() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 100, T.plusSeconds(3600)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void reportsOnlyTheOverlappingQuantityWhenTheDepositIsSmaller() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 60, T.plusSeconds(3600)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).containsExactly(new RoundTrip("Alrik", FEDERN, 60));
	}

	@Test
	void reportsOnlyTheOverlappingQuantityWhenTheWithdrawalIsSmaller() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 50, T), deposit("Alrik", FEDERN, 80, T.plusSeconds(3600)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).containsExactly(new RoundTrip("Alrik", FEDERN, 50));
	}

	@Test
	void reportsADepositExactlyFortyEightHoursAfterTheWithdrawal() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 100, T.plus(RoundTripDetector.WINDOW)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void leavesADepositAloneOnceTheWindowHasPassed() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", FEDERN, 100, T.plus(RoundTripDetector.WINDOW).plusSeconds(60)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).isEmpty();
	}

	@Test
	void matchesADepositAgainstTheOldestOpenLotThatIsStillInsideTheWindow() {
		List<ResolvedStorageEntry> entries = List
				.of(withdrawal("Alrik", FEDERN, 100, T), withdrawal("Alrik", FEDERN, 100, T.plusSeconds(47 * 3600)), deposit("Alrik", FEDERN, 100, T.plusSeconds(49 * 3600)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void keepsItemsApart() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Alrik", HARZ, 100, T.plusSeconds(3600)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).isEmpty();
	}

	@Test
	void keepsAvatarsApart() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", FEDERN, 100, T), deposit("Brynn", FEDERN, 100, T.plusSeconds(3600)));

		List<RoundTrip> alriksRoundTrips = RoundTripDetector.detect("Alrik", entries);
		List<RoundTrip> brynnsRoundTrips = RoundTripDetector.detect("Brynn", entries);

		assertThat(alriksRoundTrips).isEmpty();
		assertThat(brynnsRoundTrips).isEmpty();
	}

	@Test
	void countsASameMinuteWithdrawalAndDepositAsARoundTripWhateverTheirOrderInTheList() {
		List<ResolvedStorageEntry> entries = List.of(deposit("Alrik", FEDERN, 100, T), withdrawal("Alrik", FEDERN, 100, T));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).containsExactly(new RoundTrip("Alrik", FEDERN, 100));
	}

	@Test
	void ignoresAGoodOutsideTheTraderTier() {
		List<ResolvedStorageEntry> entries = List.of(withdrawal("Alrik", KUPFERERZ, 100, T), deposit("Alrik", KUPFERERZ, 100, T.plusSeconds(3600)));

		List<RoundTrip> roundTrips = RoundTripDetector.detect("Alrik", entries);

		assertThat(roundTrips).isEmpty();
	}

	private static ResolvedStorageEntry withdrawal(String avatar, EvergoreItem item, int quantity, Instant at) {
		return new ResolvedStorageEntry(new StorageEntry(at, avatar, quantity, item.ingameName, 100, TransferType.ENTNAHME), item);
	}

	private static ResolvedStorageEntry deposit(String avatar, EvergoreItem item, int quantity, Instant at) {
		return new ResolvedStorageEntry(new StorageEntry(at, avatar, quantity, item.ingameName, 100, TransferType.EINLAGERUNG), item);
	}
}
