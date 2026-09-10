package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

public record GuildShare(double donation, double craftSubsidy) {
	public static final GuildShare NOTHING = new GuildShare(0, 0);

	public GuildShare inWholeGold() {
		return new GuildShare(Math.round(donation), Math.round(craftSubsidy));
	}

	public GuildShare plus(GuildShare other) {
		return new GuildShare(donation + other.donation, craftSubsidy + other.craftSubsidy);
	}
}
