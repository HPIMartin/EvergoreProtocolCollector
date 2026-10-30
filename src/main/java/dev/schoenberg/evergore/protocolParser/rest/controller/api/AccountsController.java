package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.List;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.ListAccounts;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AccountWire;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(AccountsController.PATH)
public class AccountsController {
	public static final String PATH = "/api/v1/admin/accounts";

	private final ListAccounts listAccounts;

	public AccountsController(ListAccounts listAccounts) {
		this.listAccounts = listAccounts;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public List<AccountWire> accounts() {
		return listAccounts.all().stream().map(listing -> new AccountWire(listing.name(), listing.resting(), listing.latestLogin().orElse(null))).toList();
	}
}
