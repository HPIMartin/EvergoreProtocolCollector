package dev.schoenberg.evergore.protocolParser.database;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.TransferTypeVisitor;

import static java.util.Arrays.stream;

public class TransferTypeDatabaseVisitor implements TransferTypeVisitor<String> {
	@Override
	public String place() {
		return "Einlagerung";
	}

	@Override
	public String withdrawl() {
		return "Entnahme";
	}

	public TransferType convert(String type) {
		return stream(TransferType.values()).filter(x -> x.accept(this).equals(type)).findFirst().orElseThrow(() -> new RuntimeException("Unknown TransferType: " + type));
	}

	public String convert(TransferType type) {
		return type.accept(this);
	}
}
