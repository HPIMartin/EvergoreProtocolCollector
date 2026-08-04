package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import jakarta.inject.Singleton;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.TransferTypeVisitor;

@Singleton
public class TransferTypeWireNames implements TransferTypeVisitor<String> {
	public static final String DEPOSIT = "DEPOSIT";
	public static final String WITHDRAWAL = "WITHDRAWAL";

	public String of(TransferType type) {
		return type.accept(this);
	}

	@Override
	public String place() {
		return DEPOSIT;
	}

	@Override
	public String withdrawl() {
		return WITHDRAWAL;
	}
}
