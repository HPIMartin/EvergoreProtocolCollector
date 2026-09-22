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
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Ingredient;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.NotCraftable;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.Published;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.Unread;

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
		RecipeConsumption consumption = new RecipeConsumption();

		for (ResolvedStorageEntry resolved : sorted) {
			EvergoreItem item = resolved.item();
			StorageEntry entry = resolved.entry();

			if (item.category != HANDWERKSMATERIAL) {
				if (entry.type() == TransferType.EINLAGERUNG) {
					consumeIngredientLots(item, entry.quantity(), entry.timeStamp(), openLots, consumption);
				}
				continue;
			}

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

	private static void consumeIngredientLots(EvergoreItem product, int depositedQuantity, Instant at, Map<EvergoreItem, Deque<Lot>> openLots, RecipeConsumption consumption) {
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
			case Unread _ -> {
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
			int consumedSoFar = ceilingDivide(depositedSoFar * ingredient.amount, recipe.amount);
			Integer attributedBefore = ingredientsAttributed.computeIfAbsent(product, ignored -> new TreeMap<>()).put(ingredient.item, consumedSoFar);
			return consumedSoFar - (attributedBefore == null ? 0 : attributedBefore);
		}
	}

	private static int ceilingDivide(int dividend, int divisor) {
		return (dividend + divisor - 1) / divisor;
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
