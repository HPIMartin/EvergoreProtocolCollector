package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.EvaluationResult;
import dev.schoenberg.evergore.protocolParser.application.LastRunStatus;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTrip;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTripAbstention;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AdminStatus;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.RoundTripAbstentionWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.RoundTripWire;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.ACHAT_ARMBRUST;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.FEDERN;
import static org.assertj.core.api.Assertions.assertThat;

class AdminStatusControllerTest {
	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final LastRunStatus lastRunStatus = new LastRunStatus();
	private final AdminStatusController tested = new AdminStatusController(
			new AvatarContributions(new KnownAvatars(bankRepo, storageRepo), new FakeMetaInformationRepository(), bankRepo, storageRepo), lastRunStatus);

	@Test
	void listsUnknownItemsAndUnrefreshedMembersInGermanOrderEachNameOnce() {
		record(List.of("Zunder", "Äxtchen", "Zunder"), List.of("Zeder", "Ähre", "Zeder"), List.of(), List.of());

		AdminStatus status = tested.status();

		assertThat(status.unknownItemNames()).containsExactly("Äxtchen", "Zunder");
		assertThat(status.failedAvatarNames()).containsExactly("Ähre", "Zeder");
	}

	@Test
	void listsRoundTripsAndAbstentionsByAvatarInGermanOrderThenByItem() {
		record(List.of(), List.of(), List.of(new RoundTrip("Zorn", FEDERN, 1), new RoundTrip("Ärger", FEDERN, 1), new RoundTrip("Ärger", ACHAT_ARMBRUST, 2)),
				List.of(new RoundTripAbstention("Zorn", FEDERN), new RoundTripAbstention("Ärger", FEDERN), new RoundTripAbstention("Ärger", ACHAT_ARMBRUST)));

		AdminStatus status = tested.status();

		assertThat(status.roundTrips())
				.containsExactly(new RoundTripWire("Ärger", "Achat-Armbrust", 2), new RoundTripWire("Ärger", "Federn", 1), new RoundTripWire("Zorn", "Federn", 1));
		assertThat(status.roundTripAbstentions())
				.containsExactly(new RoundTripAbstentionWire("Ärger", "Achat-Armbrust"), new RoundTripAbstentionWire("Ärger", "Federn"),
						new RoundTripAbstentionWire("Zorn", "Federn"));
	}

	private void record(List<String> unknown, List<String> failed, List<RoundTrip> trips, List<RoundTripAbstention> abstentions) {
		lastRunStatus.recordSuccessfulRecompute(Instant.parse("2026-06-21T12:00:00Z"), new EvaluationResult(unknown, List.of(), failed, trips, abstentions));
	}
}
