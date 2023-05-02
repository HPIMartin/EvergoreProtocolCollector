package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.*;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;

public class StorageEntry {
	public final Instant timeStamp;
	public final String avatar;
	public final int quantity;
	public final String name;
	public final int quality;
	public final TransferType type;

	public StorageEntry(Instant timeStamp, String avatar, int quantity, String name, int quality, TransferType type) {
		this.timeStamp = timeStamp;
		this.avatar = avatar;
		this.quantity = quantity;
		this.name = name;
		this.quality = quality;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof StorageEntry && ((StorageEntry) obj).stupidMerge().equals(stupidMerge());
	}

	@Override
	public int hashCode() {
		return stupidMerge().hashCode();
	}

	private String stupidMerge() {
		return timeStamp + ":" + avatar + ":" + quantity + ":" + name + ":" + quality + ":" + type;
	}
}
