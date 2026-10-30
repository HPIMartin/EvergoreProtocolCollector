package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.List;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.ListAccounts;
import dev.schoenberg.evergore.protocolParser.application.ResetPassword;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AccountWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.ResetWire;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(AccountsController.PATH)
public class AccountsController {
	public static final String PATH = "/api/v1/admin/accounts";

	private final ListAccounts listAccounts;
	private final ResetPassword resetPassword;

	public AccountsController(ListAccounts listAccounts, ResetPassword resetPassword) {
		this.listAccounts = listAccounts;
		this.resetPassword = resetPassword;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public List<AccountWire> accounts() {
		return listAccounts.all().stream().map(listing -> new AccountWire(listing.name(), listing.resting(), listing.latestLogin().orElse(null))).toList();
	}

	@Post("/{name}/reset")
	@Produces(APPLICATION_JSON)
	public HttpResponse<ResetWire> reset(@PathVariable("name") String name) {
		return resetPassword
				.forAccount(name)
				.map(issued -> HttpResponse.ok(new ResetWire(issued)).header("Cache-Control", "no-store"))
				.orElseGet(() -> HttpResponse.status(HttpStatus.NOT_FOUND));
	}
}
