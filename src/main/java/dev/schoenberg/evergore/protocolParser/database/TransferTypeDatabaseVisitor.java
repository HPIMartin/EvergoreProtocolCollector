package dev.schoenberg.evergore.protocolParser.database;

import static java.util.Arrays.*;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.*;

public class TransferTypeDatabaseVisitor implements TransfertTypeVisitor<String> {
	@Override
	public String place() {
		return "Einlagerung";
	}

	@Override
	public String withdrawl() {
		return "Entnahme";
	}

	public TransferType convert(String type) {
		return stream(TransferType.values()).filter(x -> x.accept(this).equals(type)).findFirst()
				.orElseThrow(() -> new RuntimeException("Unknown TransferType: " + type));
	}

	public String convert(TransferType type) {
		return type.accept(this);
	}
}
