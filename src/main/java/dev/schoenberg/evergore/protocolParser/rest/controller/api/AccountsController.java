package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.List;
import java.util.Optional;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.AccountListing;
import dev.schoenberg.evergore.protocolParser.application.ChangeRole;
import dev.schoenberg.evergore.protocolParser.application.ListAccounts;
import dev.schoenberg.evergore.protocolParser.application.ResetPassword;
import dev.schoenberg.evergore.protocolParser.application.RoleChangeOutcome;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AccountWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.LoginRefusal;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.ResetWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.RoleRequest;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.RoleWireNames;
import dev.schoenberg.evergore.protocolParser.rest.filter.LoginAttribute;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(AccountsController.PATH)
public class AccountsController {
	public static final String PATH = "/api/v1/admin/accounts";

	private final ListAccounts listAccounts;
	private final ResetPassword resetPassword;
	private final ChangeRole changeRole;

	public AccountsController(ListAccounts listAccounts, ResetPassword resetPassword, ChangeRole changeRole) {
		this.listAccounts = listAccounts;
		this.resetPassword = resetPassword;
		this.changeRole = changeRole;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public List<AccountWire> accounts(HttpRequest<?> request) {
		return listAccounts.all(loginOf(request)).stream().map(AccountsController::wireOf).toList();
	}

	private static AccountWire wireOf(AccountListing listing) {
		return new AccountWire(listing.name(), RoleWireNames.of(listing.role()), listing.own(), listing.resting(), listing.latestLogin().orElse(null));
	}

	@Post("/{name}/reset")
	@Produces(APPLICATION_JSON)
	public HttpResponse<ResetWire> reset(@PathVariable("name") String name) {
		return resetPassword
				.forAccount(name)
				.map(issued -> HttpResponse.ok(new ResetWire(issued)).header("Cache-Control", "no-store"))
				.orElseGet(() -> HttpResponse.status(HttpStatus.NOT_FOUND));
	}

	@Post("/{name}/role")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	public HttpResponse<?> role(HttpRequest<?> request, @PathVariable("name") String name, @Body @Nullable RoleRequest body) {
		Optional<Role> role = body == null ? Optional.empty() : RoleWireNames.parse(body.role());
		if (role.isEmpty()) {
			return HttpResponse.badRequest(LoginRefusal.UNKNOWN_ROLE);
		}

		return changeRole.to(name, role.get(), loginOf(request)).accept(new RoleAnswer());
	}

	private static Login loginOf(HttpRequest<?> request) {
		return request.getAttribute(LoginAttribute.NAME, Login.class).orElseThrow(AccessNotAllowed::new);
	}

	private static final class RoleAnswer implements RoleChangeOutcome.Visitor<HttpResponse<?>> {
		@Override
		public HttpResponse<?> onChanged() {
			return HttpResponse.noContent();
		}

		@Override
		public HttpResponse<?> onUnchanged() {
			return HttpResponse.serverError();
		}

		@Override
		public HttpResponse<?> onNoAccount() {
			return HttpResponse.status(HttpStatus.NOT_FOUND);
		}

		@Override
		public HttpResponse<?> onOwnAdminRole() {
			return HttpResponse.serverError();
		}
	}
}
