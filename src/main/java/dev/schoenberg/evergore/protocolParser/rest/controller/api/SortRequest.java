package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.Map;

import io.micronaut.http.exceptions.HttpStatusException;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankSortKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerSort;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageSortKey;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.ASCENDING;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.DESCENDING;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;

public final class SortRequest {
	private static final String TIMESTAMP = "timestamp";
	private static final String DESCENDING_ORDER = "descending";
	private static final String AVATAR = "avatar";
	private static final String TRANSFER_TYPE = "transferType";

	public static final String SORT = "sort";
	public static final String DIRECTION = "direction";
	public static final String DEFAULT_SORT = TIMESTAMP;
	public static final String DEFAULT_DIRECTION = DESCENDING_ORDER;

	private static final Map<String, BankSortKey> BANK_COLUMNS = Map
			.of(TIMESTAMP, BankSortKey.TIMESTAMP, AVATAR, BankSortKey.AVATAR, "amount", BankSortKey.AMOUNT, TRANSFER_TYPE, BankSortKey.TRANSFER_TYPE);
	private static final Map<String, StorageSortKey> STORAGE_COLUMNS = Map
			.of(TIMESTAMP, StorageSortKey.TIMESTAMP, AVATAR, StorageSortKey.AVATAR, "quantity", StorageSortKey.QUANTITY, "name", StorageSortKey.NAME, "quality",
					StorageSortKey.QUALITY, TRANSFER_TYPE, StorageSortKey.TRANSFER_TYPE);
	private static final Map<String, SortDirection> DIRECTIONS = Map.of("ascending", ASCENDING, DESCENDING_ORDER, DESCENDING);

	private SortRequest() {}

	public static LedgerSort<BankSortKey> bankSortOf(String column, String direction) {
		return sortOf(BANK_COLUMNS, column, direction);
	}

	public static LedgerSort<StorageSortKey> storageSortOf(String column, String direction) {
		return sortOf(STORAGE_COLUMNS, column, direction);
	}

	private static <K> LedgerSort<K> sortOf(Map<String, K> columns, String column, String direction) {
		K key = columns.get(column);
		SortDirection order = DIRECTIONS.get(direction);
		if (key == null || order == null) {
			throw new HttpStatusException(BAD_REQUEST, "This ledger cannot be sorted that way");
		}
		return new LedgerSort<>(key, order);
	}
}
