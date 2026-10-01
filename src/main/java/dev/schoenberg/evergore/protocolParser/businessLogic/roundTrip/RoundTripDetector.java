package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Ingredient;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.NotCraftable;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.Published;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.Unread;

public final class RoundTripDetector {
	public static final Duration WINDOW = Duration.ofHours(48);

	private RoundTripDetector() {}

	public static RoundTripReport detect(String avatar, List<ResolvedStorageEntry> entries) {
		List<ResolvedStorageEntry> sorted = entries
				.stream()
				.filter(resolved -> resolved.entry().avatar().equals(avatar))
				.sorted(Comparator
						.comparing((ResolvedStorageEntry resolved) -> resolved.entry().timeStamp())
						.thenComparingInt(resolved -> resolved.entry().type() == TransferType.ENTNAHME ? 0 : 1))
				.toList();

		Map<EvergoreItem, Deque<Lot>> openLots = new TreeMap<>();
		Map<EvergoreItem, Integer> roundTripQuantities = new TreeMap<>();
		Set<EvergoreItem> abstainedItems = new TreeSet<>();
		RecipeConsumption consumption = new RecipeConsumption();

		for (ResolvedStorageEntry resolved : sorted) {
			EvergoreItem item = resolved.item();
			StorageEntry entry = resolved.entry();

			if (entry.type() == TransferType.ENTNAHME) {
				if (item.creditsMoreThanItsWithdrawalCosts()) {
					openLots.computeIfAbsent(item, ignored -> new ArrayDeque<>()).addLast(new Lot(entry.timeStamp(), entry.quantity()));
				}
				continue;
			}

			int crafted = entry.quantity();
			if (item.creditsMoreThanItsWithdrawalCosts()) {
				Deque<Lot> lots = openLots.computeIfAbsent(item, ignored -> new ArrayDeque<>());
				dropExpiredLots(lots, entry.timeStamp());
				int matched = consumeLots(lots, crafted);
				if (matched > 0) {
					roundTripQuantities.merge(item, matched, Integer::sum);
				}
				crafted -= matched;
			}
			if (crafted > 0) {
				handleProductDeposit(item, crafted, entry.timeStamp(), openLots, abstainedItems, consumption);
			}
		}

		List<RoundTrip> roundTrips = roundTripQuantities
				.entrySet()
				.stream()
				.filter(entry -> !abstainedItems.contains(entry.getKey()))
				.map(entry -> new RoundTrip(avatar, entry.getKey(), entry.getValue()))
				.toList();
		List<RoundTripAbstention> abstentions = abstainedItems.stream().map(item -> new RoundTripAbstention(avatar, item)).toList();
		return new RoundTripReport(roundTrips, abstentions);
	}

	private static void handleProductDeposit(EvergoreItem product, int depositedQuantity, Instant at, Map<EvergoreItem, Deque<Lot>> openLots, Set<EvergoreItem> abstainedItems,
			RecipeConsumption consumption) {
		switch (product.recipe) {
			case Published published -> {
				int depositedSoFar = consumption.deposited(product, depositedQuantity);
				for (Ingredient ingredient : published.ingredients) {
					int newlyConsumed = consumption.attribute(product, depositedSoFar, published, ingredient);
					Deque<Lot> lots = openLots.get(ingredient.item);
					if (lots == null || lots.isEmpty()) {
						continue;
					}
					dropExpiredLots(lots, at);
					consumeLots(lots, newlyConsumed);
				}
			}
			case NotCraftable _ -> {
			}
			case Unread _ -> abstainOpenLots(at, openLots, abstainedItems);
		}
	}

	private static void abstainOpenLots(Instant at, Map<EvergoreItem, Deque<Lot>> openLots, Set<EvergoreItem> abstainedItems) {
		for (Map.Entry<EvergoreItem, Deque<Lot>> entry : openLots.entrySet()) {
			dropExpiredLots(entry.getValue(), at);
			if (!entry.getValue().isEmpty()) {
				abstainedItems.add(entry.getKey());
			}
		}
	}

	private static final class RecipeConsumption {
		private final Map<EvergoreItem, Integer> productsDeposited = new TreeMap<>();
		private final Map<EvergoreItem, Map<EvergoreItem, Integer>> ingredientsAttributed = new TreeMap<>();

		private int deposited(EvergoreItem product, int depositedQuantity) {
			return productsDeposited.merge(product, depositedQuantity, Integer::sum);
		}

		private int attribute(EvergoreItem product, int depositedSoFar, Published recipe, Ingredient ingredient) {
			int consumedSoFar = ceilingDivide((long) depositedSoFar * ingredient.amount, recipe.amount);
			Integer attributedBefore = ingredientsAttributed.computeIfAbsent(product, ignored -> new TreeMap<>()).put(ingredient.item, consumedSoFar);
			return consumedSoFar - (attributedBefore == null ? 0 : attributedBefore);
		}
	}

	private static int ceilingDivide(long dividend, int divisor) {
		return Math.toIntExact((dividend + divisor - 1) / divisor);
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
