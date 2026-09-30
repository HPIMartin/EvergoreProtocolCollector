package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.util.Collection;
import java.util.Optional;

public record Contribution(long bankDeposited, long bankWithdrawn, double storageDeposited, double storageWithdrawn, Optional<GuildShare> guildShare) {
	public static final Contribution NOTHING = new Contribution(0, 0, 0, 0, Optional.of(GuildShare.NOTHING));

	public double net() {
		return bankDeposited - bankWithdrawn + storageDeposited - storageWithdrawn;
	}

	public WholeGoldContribution inWholeGold() {
		return new WholeGoldContribution(bankDeposited, bankWithdrawn, wholeGoldOf(storageDeposited), wholeGoldOf(storageWithdrawn), wholeGoldOf(net()),
				guildShare.map(this::wholeGoldShareOf));
	}

	public Contribution plus(Contribution other) {
		return new Contribution(bankDeposited + other.bankDeposited, bankWithdrawn + other.bankWithdrawn, storageDeposited + other.storageDeposited,
				storageWithdrawn + other.storageWithdrawn, sumOf(guildShare, other.guildShare));
	}

	public static Contribution sumOf(Collection<Contribution> contributions) {
		return contributions.stream().reduce(NOTHING, Contribution::plus);
	}

	private static Optional<GuildShare> sumOf(Optional<GuildShare> one, Optional<GuildShare> other) {
		return one.flatMap(share -> other.map(share::plus));
	}

	private WholeGoldShare wholeGoldShareOf(GuildShare share) {
		double storageValue = storageDeposited + share.donation() - share.craftSubsidy() - storageWithdrawn;
		double balance = net() + share.donation() - share.craftSubsidy();
		return new WholeGoldShare(wholeGoldOf(share.donation()), wholeGoldOf(share.craftSubsidy()), wholeGoldOf(storageValue), wholeGoldOf(balance));
	}

	private static long wholeGoldOf(double exact) {
		return exact < 0 ? -Math.round(-exact) : Math.round(exact);
	}
}
