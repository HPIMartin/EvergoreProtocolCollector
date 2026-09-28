package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.Map;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ServiceRequests;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class AddressSteps {
	private static final int BAD_REQUEST = 400;
	private static final int UNAUTHORIZED = 401;

	private final ServiceRequests requests;
	private Answer answer;

	public AddressSteps(ServiceRequests requests) {
		this.requests = requests;
	}

	@ParameterType("climbs up out of the dashboard's own files and back down to the overview" + "|makes that same climb in a disguised spelling"
			+ "|starts the overview's address with a doubled slash" + "|leads to the overview but contains a garbled character"
			+ "|leads to the bank ledger of \"Aurora\" but contains a garbled character")
	public String trickedAddress(String kind) {
		return switch (kind) {
			case "climbs up out of the dashboard's own files and back down to the overview" -> "/assets/../overview";
			case "makes that same climb in a disguised spelling" -> "/assets/%2e%2e/overview";
			case "starts the overview's address with a doubled slash" -> "//overview";
			case "leads to the overview but contains a garbled character" -> "/overview%zz";
			default -> "/avatars/Aurora%zz/bank";
		};
	}

	@When("a client asks for an address that {trickedAddress} without the guild's token")
	public void aClientAsksForAnAddressWithoutTheToken(String address) {
		answer = requests.get(address, Map.of());
	}

	@Then("the operator finds the address refused like any request without the token")
	public void theAddressIsRefusedLikeAnyRequestWithoutTheToken() {
		int withoutToken = requests.get(OVERVIEW, Map.of()).status();

		assertThat(withoutToken).isEqualTo(UNAUTHORIZED);
		assertThat(answer.status()).isEqualTo(withoutToken);
	}

	@Then("the operator finds the address refused as garbled")
	public void theAddressIsRefusedAsGarbled() {
		assertThat(answer.status()).isEqualTo(BAD_REQUEST);
	}
}
