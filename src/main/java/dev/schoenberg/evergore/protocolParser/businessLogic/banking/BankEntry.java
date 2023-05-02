package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.time.*;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;

public class BankEntry {
	public final Instant timeStamp;
	public final String avatar;
	public final int amount;
	public final TransferType type;

	public BankEntry(Instant timeStamp, String avatar, int amount, TransferType type) {
		this.timeStamp = timeStamp;
		this.avatar = avatar;
		this.amount = amount;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BankEntry && ((BankEntry) obj).stupidMerge().equals(stupidMerge());
	}

	@Override
	public int hashCode() {
		return stupidMerge().hashCode();
	}

	private String stupidMerge() {
		return timeStamp + ":" + avatar + ":" + amount + ":" + type;
	}
}
