package dev.schoenberg.evergore.protocolParser.acceptance.service;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;

@Singleton
@Requires(env = AcceptanceEnvironment.NAME)
class UnreadableBankLedger implements BeanCreatedEventListener<BankRepository> {
	private final LedgerFaults faults;

	UnreadableBankLedger(LedgerFaults faults) {
		this.faults = faults;
	}

	@Override
	public BankRepository onCreated(BeanCreatedEvent<BankRepository> event) {
		return faults.guardLedger(BankRepository.class, event.getBean());
	}
}
