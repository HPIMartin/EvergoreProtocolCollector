package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.util.Collection;

public record Contribution(long bankDeposited, long bankWithdrawn, double storageDeposited, double storageWithdrawn) {
	public static final Contribution NOTHING = new Contribution(0, 0, 0, 0);

	public double net() {
		return bankDeposited - bankWithdrawn + storageDeposited - storageWithdrawn;
	}

	public Contribution inWholeGold() {
		return new Contribution(bankDeposited, bankWithdrawn, Math.round(storageDeposited), Math.round(storageWithdrawn));
	}

	public Contribution plus(Contribution other) {
		return new Contribution(bankDeposited + other.bankDeposited, bankWithdrawn + other.bankWithdrawn, storageDeposited + other.storageDeposited,
				storageWithdrawn + other.storageWithdrawn);
	}

	public static Contribution sumOf(Collection<Contribution> contributions) {
		return contributions.stream().reduce(NOTHING, Contribution::plus);
	}
}
