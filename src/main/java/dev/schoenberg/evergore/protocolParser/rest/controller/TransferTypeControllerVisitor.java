package dev.schoenberg.evergore.protocolParser.rest.controller;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.*;

public class TransferTypeControllerVisitor implements TransfertTypeVisitor<String> {
	@Override
	public String place() {
		return "Einlagerung";
	}

	@Override
	public String withdrawl() {
		return "Entnahme";
	}
}
