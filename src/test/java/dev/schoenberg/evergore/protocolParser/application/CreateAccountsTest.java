package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAccountsTest {
	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final FakeAccounts accounts = new FakeAccounts();
	private final CountingGenerator generator = new CountingGenerator();
	private final MarkingHasher hasher = new MarkingHasher();
	private final RecordedPasswords issued = new RecordedPasswords();
	private final LoggerSpy logger = new LoggerSpy();
	private final CreateAccounts tested = new CreateAccounts(new KnownAvatars(bankRepo, storageRepo), accounts, generator, hasher, issued, logger);

	@Test
	void givesAnAvatarTheBankLedgerNamesAnAccountThatBeginsWithAReset() {
		bankRepo.seedAvatars(List.of("Aurora"));

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).containsExactly(new Account("Aurora", "hashed:Generated-1!", PasswordState.ISSUED, Role.MEMBER, Optional.empty()));
	}

	@Test
	void givesAnAvatarOnlyTheStorageLedgerNamesAnAccountToo() {
		storageRepo.seedAvatars(List.of("Brynja"));

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).extracting(Account::avatar).containsExactly("Brynja");
	}

	@Test
	void givesAnAvatarBothLedgersNameOneAccountAndOnePassword() {
		bankRepo.seedAvatars(List.of("Aurora"));
		storageRepo.seedAvatars(List.of("Aurora"));

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).hasSize(1);
		assertThat(issued.all()).hasSize(1);
	}

	@Test
	void storesOnlyTheHashOfTheIssuedPassword() {
		bankRepo.seedAvatars(List.of("Aurora"));

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).extracting(Account::passwordHash).noneMatch(hash -> hash.equals("Generated-1!"));
	}

	@Test
	void handsTheClearPasswordToTheIssuedPasswordsTogetherWithTheAvatar() {
		bankRepo.seedAvatars(List.of("Aurora"));

		tested.forEveryKnownAvatar();

		assertThat(issued.all()).containsExactly(Map.entry("Aurora", "Generated-1!"));
	}

	@Test
	void issuesEveryAccountAPasswordOfItsOwn() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));

		tested.forEveryKnownAvatar();

		assertThat(issued.all()).containsExactly(Map.entry("Aurora", "Generated-1!"), Map.entry("Boreas", "Generated-2!"));
	}

	@Test
	void leavesAnExistingAccountAsItWasAndIssuesNoNewPassword() {
		bankRepo.seedAvatars(List.of("Aurora"));
		tested.forEveryKnownAvatar();
		Account first = accounts.all().getFirst();
		accounts.replacePassword("aurora", "hashed:Sommer-2026!", PasswordState.CHOSEN);

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).containsExactly(new Account("Aurora", "hashed:Sommer-2026!", PasswordState.CHOSEN, first.role(), first.lastLogin()));
		assertThat(issued.all()).hasSize(1);
	}

	@Test
	void givesAnAvatarFirstReadByALaterCollectionAnAccountAndLeavesTheOthersAlone() {
		bankRepo.seedAvatars(List.of("Aurora"));
		tested.forEveryKnownAvatar();
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));

		tested.forEveryKnownAvatar();

		assertThat(issued.all()).containsExactly(Map.entry("Aurora", "Generated-1!"), Map.entry("Boreas", "Generated-2!"));
	}

	@Test
	void givesTwoAvatarsThatDifferOnlyInCapitalsOneAccountAndOnePassword() {
		bankRepo.seedAvatars(List.of("Aurora", "AURORA"));

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).hasSize(1);
		assertThat(issued.all()).hasSize(1);
	}

	@ParameterizedTest
	@ValueSource(strings = {"admin", "Admin", "ADMIN", "aDmIn"})
	void givesAnAvatarNamedAdminInAnyCapitalsNoAccountAndNoPassword(String avatar) {
		bankRepo.seedAvatars(List.of(avatar));

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).isEmpty();
		assertThat(issued.all()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"admin", "Admin", "ADMIN"})
	void namesTheAvatarTheAdminKeyKeepsFromAnAccountAsTheLedgerWritesIt(String avatar) {
		bankRepo.seedAvatars(List.of(avatar));

		tested.forEveryKnownAvatar();

		assertThat(logger.infoMessages()).containsExactly("No account for the avatar " + avatar + ": the name admin logs in with the admin key.");
	}

	@Test
	void stillGivesTheOtherAvatarsAnAccountBesideOneNamedAdmin() {
		bankRepo.seedAvatars(List.of("Admin", "Aurora"));

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).extracting(Account::avatar).containsExactly("Aurora");
	}

	@Test
	void logsNothingWhileNoAvatarIsNamedAdmin() {
		bankRepo.seedAvatars(List.of("Aurora"));

		tested.forEveryKnownAvatar();

		assertThat(logger.infoMessages()).isEmpty();
	}

	@Test
	void givesTheOtherAvatarsTheirAccountWhenTheHashOfOneAvatarFailsAndLogsThatAvatarAtError() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		hasher.failFor("Generated-1!");

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).extracting(Account::avatar).containsExactly("Boreas");
		assertThat(logger.errorMessages()).containsExactly("The account of the avatar Aurora could not be created; the other avatars still get theirs.");
		assertThat(logger.errorThrowables()).hasSize(1);
	}

	@Test
	void givesTheOtherAvatarsTheirAccountWhenTheInsertOfOneAvatarFailsAndLogsThatAvatarAtError() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		accounts.failToAdd("aurora");

		tested.forEveryKnownAvatar();

		assertThat(accounts.all()).extracting(Account::avatar).containsExactly("Boreas");
		assertThat(logger.errorMessages()).containsExactly("The account of the avatar Aurora could not be created; the other avatars still get theirs.");
		assertThat(issued.all()).containsExactly(Map.entry("Boreas", "Generated-2!"));
	}

	@Test
	void hashesOnTheThreadThatCallsIt() {
		bankRepo.seedAvatars(List.of("Aurora"));

		tested.forEveryKnownAvatar();

		assertThat(hasher.threadsThatHashed()).containsExactly(Thread.currentThread());
	}

	private static class FakeAccounts implements AccountRepository {
		private final Map<String, Account> byNameKey = new LinkedHashMap<>();
		private String nameKeyThatFailsToAdd = "";

		List<Account> all() {
			return List.copyOf(byNameKey.values());
		}

		void failToAdd(String nameKey) {
			nameKeyThatFailsToAdd = nameKey;
		}

		@Override
		public Optional<Account> findByNameKey(String nameKey) {
			return Optional.ofNullable(byNameKey.get(nameKey));
		}

		@Override
		public boolean addIfAbsent(Account account) {
			if (account.nameKey().equals(nameKeyThatFailsToAdd)) {
				throw new IllegalStateException("The insert failed.");
			}
			return byNameKey.putIfAbsent(account.nameKey(), account) == null;
		}

		@Override
		public void replacePassword(String nameKey, String passwordHash, PasswordState state) {
			Account held = byNameKey.get(nameKey);
			byNameKey.put(nameKey, new Account(held.avatar(), passwordHash, state, held.role(), held.lastLogin()));
		}

		@Override
		public boolean replaceIssuedPassword(String nameKey, String passwordHash) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void recordLastLogin(String nameKey, Instant at) {
			throw new UnsupportedOperationException();
		}
	}

	private static class CountingGenerator implements PasswordGenerator {
		private final AtomicInteger issuedSoFar = new AtomicInteger();

		@Override
		public String generate() {
			return "Generated-" + issuedSoFar.incrementAndGet() + "!";
		}
	}

	private static class MarkingHasher implements PasswordHasher {
		private final List<Thread> threadsThatHashed = new ArrayList<>();
		private String clearThatFails = "";

		List<Thread> threadsThatHashed() {
			return threadsThatHashed;
		}

		void failFor(String clear) {
			clearThatFails = clear;
		}

		@Override
		public String hash(String clear) {
			if (clear.equals(clearThatFails)) {
				throw new IllegalStateException("The hash failed.");
			}
			threadsThatHashed.add(Thread.currentThread());
			return "hashed:" + clear;
		}

		@Override
		public boolean verify(String clear, String encoded) {
			return encoded.equals("hashed:" + clear);
		}
	}

	private static class RecordedPasswords implements IssuedPasswords {
		private final List<Map.Entry<String, String>> inOrder = new ArrayList<>();

		List<Map.Entry<String, String>> all() {
			return inOrder;
		}

		@Override
		public void issue(String avatar, String clear) {
			inOrder.add(Map.entry(avatar, clear));
		}
	}
}
