package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.List;
import java.util.stream.Stream;

import io.micronaut.http.exceptions.HttpStatusException;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankSortKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerSort;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageSortKey;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.ASCENDING;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.DESCENDING;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SortRequestTest {
	@Test
	void namesEveryBankColumnAsTheWireNamesItsField() {
		List<BankSortKey> keys = Stream.of("timestamp", "avatar", "amount", "transferType").map(column -> SortRequest.bankSortOf(column, "ascending").key()).toList();

		assertThat(keys).containsExactly(BankSortKey.TIMESTAMP, BankSortKey.AVATAR, BankSortKey.AMOUNT, BankSortKey.TRANSFER_TYPE);
	}

	@Test
	void namesEveryStorageColumnAsTheWireNamesItsField() {
		List<StorageSortKey> keys = Stream
				.of("timestamp", "avatar", "quantity", "name", "quality", "transferType")
				.map(column -> SortRequest.storageSortOf(column, "ascending").key())
				.toList();

		assertThat(keys)
				.containsExactly(StorageSortKey.TIMESTAMP, StorageSortKey.AVATAR, StorageSortKey.QUANTITY, StorageSortKey.NAME, StorageSortKey.QUALITY,
						StorageSortKey.TRANSFER_TYPE);
	}

	@Test
	void namesBothDirectionsInWords() {
		List<SortDirection> directions = Stream.of("ascending", "descending").map(direction -> SortRequest.storageSortOf("quantity", direction).direction()).toList();

		assertThat(directions).containsExactly(ASCENDING, DESCENDING);
	}

	@Test
	void defaultsToTheNewestMovementFirst() {
		LedgerSort<StorageSortKey> sort = SortRequest.storageSortOf(SortRequest.DEFAULT_SORT, SortRequest.DEFAULT_DIRECTION);

		assertThat(sort).isEqualTo(new LedgerSort<>(StorageSortKey.TIMESTAMP, DESCENDING));
	}

	@Test
	void rejectsAColumnTheLedgerDoesNotHaveAsABadRequest() {
		Throwable rejection = catchThrowable(() -> SortRequest.bankSortOf("quantity", "ascending"));

		assertThat(rejection).isInstanceOfSatisfying(HttpStatusException.class, refused -> assertThat(refused.getStatus().getCode()).isEqualTo(BAD_REQUEST.getCode()));
	}

	@Test
	void rejectsADirectionItDoesNotKnowAsABadRequest() {
		Throwable rejection = catchThrowable(() -> SortRequest.storageSortOf("quantity", "asc"));

		assertThat(rejection).isInstanceOfSatisfying(HttpStatusException.class, refused -> assertThat(refused.getStatus().getCode()).isEqualTo(BAD_REQUEST.getCode()));
	}

	@Test
	void takesNoColumnNameInAnotherSpelling() {
		Throwable rejection = catchThrowable(() -> SortRequest.storageSortOf("Quantity", "ascending"));

		assertThat(rejection).isInstanceOfSatisfying(HttpStatusException.class, refused -> assertThat(refused.getStatus().getCode()).isEqualTo(BAD_REQUEST.getCode()));
	}
}
