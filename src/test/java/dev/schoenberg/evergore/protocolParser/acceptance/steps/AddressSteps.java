package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.Map;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientAnswer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ServiceRequests;
import dev.schoenberg.evergore.protocolParser.rest.filter.AcceptanceClientIp;

import static org.assertj.core.api.Assertions.assertThat;

public class AddressSteps {
	private static final String A_CLIENT = "198.51.100.1";
	private static final int FOUND = 302;
	private static final int BAD_REQUEST = 400;
	private static final int UNAUTHORIZED = 401;

	private final ServiceRequests requests;
	private final ClientAnswer received;

	public AddressSteps(ServiceRequests requests, ClientAnswer received) {
		this.requests = requests;
		this.received = received;
	}

	@ParameterType("climbs up out of the login page's own files and back down to the overview"
			+ "|climbs out of the login page's files and down to the overview in a disguised spelling"
			+ "|climbs out of the login page's files, written in other capitals, to the overview" + "|starts like the login page's address and continues to the overview's figures"
			+ "|starts like the health check's address and continues to the overview's figures" + "|climbs up out of the health check's address and back down to the overview"
			+ "|starts the overview's address with a doubled slash" + "|leads to the overview but contains a garbled character"
			+ "|leads to the bank ledger of \"Aurora\" but contains a garbled character")
	public String trickedAddress(String kind) {
		return switch (kind) {
			case "climbs up out of the login page's own files and back down to the overview" -> "/login/../overview";
			case "climbs out of the login page's files and down to the overview in a disguised spelling" -> "/login/%2e%2e/overview";
			case "climbs out of the login page's files, written in other capitals, to the overview" -> "/LOGIN/%2E%2E/overview";
			case "starts like the login page's address and continues to the overview's figures" -> "/login/../api/v1/avatars";
			case "starts like the health check's address and continues to the overview's figures" -> "/health/../api/v1/avatars";
			case "climbs up out of the health check's address and back down to the overview" -> "/health/../overview";
			case "starts the overview's address with a doubled slash" -> "//overview";
			case "leads to the overview but contains a garbled character" -> "/overview%zz";
			default -> "/avatars/Aurora%zz/bank";
		};
	}

	@When("a client without a login asks for an address that {trickedAddress}")
	public void aClientWithoutALoginAsksForAnAddressThat(String address) {
		received.remember(requests.get(address, Map.of(AcceptanceClientIp.HEADER, A_CLIENT, "Accept", "text/html")));
	}

	@Then("the operator finds the client sent to the login page")
	public void theOperatorFindsTheClientSentToTheLoginPage() {
		assertThat(received.current().status()).isEqualTo(FOUND);
		assertThat(received.current().header("Location")).hasValueSatisfying(location -> assertThat(location).startsWith("/login?next="));
	}

	@Then("the operator finds the client refused for lacking a login")
	public void theOperatorFindsTheClientRefusedForLackingALogin() {
		assertThat(received.current().status()).isEqualTo(UNAUTHORIZED);
	}

	@Then("the operator finds the client refused as garbled")
	public void theOperatorFindsTheClientRefusedAsGarbled() {
		assertThat(received.current().status()).isEqualTo(BAD_REQUEST);
	}
}
