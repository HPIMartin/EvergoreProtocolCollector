package dev.schoenberg.evergore.protocolParser.rest.filter;

import dev.schoenberg.evergore.protocolParser.rest.controller.api.LoginController;

final class LoginPaths {
	private LoginPaths() {}

	static boolean isOfTheLogin(String canonicalPath) {
		return canonicalPath.equals(LoginController.PATH) || canonicalPath.startsWith(LoginController.PATH + "/");
	}
}
