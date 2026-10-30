package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;

public enum ResetOrder {
	LATEST_RESET {
		@Override
		String issuedTo(Accounts accounts, String avatar) {
			return accounts.issuedTo(avatar);
		}
	},
	FIRST_RESET {
		@Override
		String issuedTo(Accounts accounts, String avatar) {
			return accounts.firstIssuedTo(avatar);
		}
	};

	abstract String issuedTo(Accounts accounts, String avatar);

	static ResetOrder of(String phrase) {
		return "first reset".equals(phrase) ? FIRST_RESET : LATEST_RESET;
	}
}
