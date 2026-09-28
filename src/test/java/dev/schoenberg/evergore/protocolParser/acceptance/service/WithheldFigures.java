package dev.schoenberg.evergore.protocolParser.acceptance.service;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;

@Singleton
@Requires(env = AcceptanceEnvironment.NAME)
class WithheldFigures implements BeanCreatedEventListener<MetaInformationRepository> {
	private final LedgerFaults faults;

	WithheldFigures(LedgerFaults faults) {
		this.faults = faults;
	}

	@Override
	public MetaInformationRepository onCreated(BeanCreatedEvent<MetaInformationRepository> event) {
		return faults.guardFigures(MetaInformationRepository.class, event.getBean());
	}
}
