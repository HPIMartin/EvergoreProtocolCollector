package dev.schoenberg.evergore.protocolParser.database;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;

import static java.util.Arrays.stream;

public class TransferTypeDatabaseVisitor {

	public TransferType convert(String type) {
		return stream(TransferType.values()).filter(x -> x.toGermanString().equals(type)).findFirst().orElseThrow(() -> new RuntimeException("Unknown TransferType: " + type));
	}

	public String convert(TransferType type) {
		return type.toGermanString();
	}
}
