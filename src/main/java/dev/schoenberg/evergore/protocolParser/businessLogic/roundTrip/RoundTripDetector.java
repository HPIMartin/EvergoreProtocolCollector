package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.HANDWERKSMATERIAL;

public final class RoundTripDetector {
	public static final Duration WINDOW = Duration.ofHours(48);

	private RoundTripDetector() {}

	public static List<RoundTrip> detect(String avatar, List<ResolvedStorageEntry> entries) {
		List<ResolvedStorageEntry> sorted = entries
				.stream()
				.filter(resolved -> resolved.entry().avatar().equals(avatar))
				.sorted(Comparator
						.comparing((ResolvedStorageEntry resolved) -> resolved.entry().timeStamp())
						.thenComparingInt(resolved -> resolved.entry().type() == TransferType.ENTNAHME ? 0 : 1))
				.toList();

		Map<EvergoreItem, Deque<Lot>> openLots = new TreeMap<>();
		Map<EvergoreItem, Integer> roundTripQuantities = new TreeMap<>();

		for (ResolvedStorageEntry resolved : sorted) {
			EvergoreItem item = resolved.item();
			if (item.category != HANDWERKSMATERIAL) {
				continue;
			}
			StorageEntry entry = resolved.entry();
			Deque<Lot> lots = openLots.computeIfAbsent(item, ignored -> new ArrayDeque<>());
			if (entry.type() == TransferType.ENTNAHME) {
				lots.addLast(new Lot(entry.timeStamp(), entry.quantity()));
			} else {
				dropExpiredLots(lots, entry.timeStamp());
				int matched = consumeLots(lots, entry.quantity());
				if (matched > 0) {
					roundTripQuantities.merge(item, matched, Integer::sum);
				}
			}
		}

		return roundTripQuantities.entrySet().stream().map(entry -> new RoundTrip(avatar, entry.getKey(), entry.getValue())).toList();
	}

	private static void dropExpiredLots(Deque<Lot> lots, Instant at) {
		while (!lots.isEmpty() && Duration.between(lots.peekFirst().openedAt, at).compareTo(WINDOW) > 0) {
			lots.pollFirst();
		}
	}

	private static int consumeLots(Deque<Lot> lots, int quantity) {
		int remaining = quantity;
		int consumed = 0;
		while (remaining > 0 && !lots.isEmpty()) {
			Lot oldest = lots.peekFirst();
			int taken = Math.min(oldest.remaining, remaining);
			oldest.remaining -= taken;
			remaining -= taken;
			consumed += taken;
			if (oldest.remaining == 0) {
				lots.pollFirst();
			}
		}
		return consumed;
	}

	private static final class Lot {
		private final Instant openedAt;
		private int remaining;

		private Lot(Instant openedAt, int remaining) {
			this.openedAt = openedAt;
			this.remaining = remaining;
		}
	}
}
