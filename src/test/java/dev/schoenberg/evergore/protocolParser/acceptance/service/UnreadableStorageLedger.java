package dev.schoenberg.evergore.protocolParser.acceptance.service;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;

@Singleton
@Requires(env = AcceptanceEnvironment.NAME)
class UnreadableStorageLedger implements BeanCreatedEventListener<StorageRepository> {
	private final LedgerFaults faults;

	UnreadableStorageLedger(LedgerFaults faults) {
		this.faults = faults;
	}

	@Override
	public StorageRepository onCreated(BeanCreatedEvent<StorageRepository> event) {
		return faults.guardLedger(StorageRepository.class, event.getBean());
	}
}
