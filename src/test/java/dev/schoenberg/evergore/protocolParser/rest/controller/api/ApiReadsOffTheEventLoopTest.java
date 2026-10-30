package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.propagation.instrument.execution.ContextPropagatingExecutorService;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.executor.ExecutorConfiguration;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.netty.util.internal.ThreadExecutorMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.LoginCookie;
import dev.schoenberg.evergore.protocolParser.RawHttpClient;
import dev.schoenberg.evergore.protocolParser.application.AccountRepository;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.PasswordHasher;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankSortKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerSort;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageSortKey;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.NO_CONTENT;
import static io.micronaut.http.HttpStatus.OK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@Property(name = "micronaut.netty.event-loops.default.num-threads", value = "1")
class ApiReadsOffTheEventLoopTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/apiReadsOffTheEventLoop/apiReadsOffTheEventLoop.sqlite");
	private static final String AVATAR = "Aurora";
	private static final String PASSWORD = "Sommer-2026!";
	private static final String CHANGED_PASSWORD = "Winter-2026!";
	private static final String ISSUED_AVATAR = "Boreas";
	private static final String ISSUED_PASSWORD = "Issued-2026!";
	private static final String BANK_ROUTE = "/api/v1/avatars/" + AVATAR + "/bank";
	private static final int HANG_GUARD_MILLIS = 30_000;

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = ApiReadsOffTheEventLoopTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private final List<Read> reads = new CopyOnWriteArrayList<>();
	private final List<Work> loginWork = new CopyOnWriteArrayList<>();
	private final ExecutorService waitingClients = Executors.newCachedThreadPool();
	private final CountDownLatch readStarted = new CountDownLatch(1);
	private final CountDownLatch lockReleased = new CountDownLatch(1);
	private final AtomicInteger waitingReads = new AtomicInteger();
	private volatile boolean readsWaitOnALock;
	private volatile boolean witnessEveryHash;

	private @Inject EmbeddedServer server;
	private @Inject @Named(TaskExecutors.IO) ExecutorConfiguration ioConfiguration;
	private @Inject @Named(TaskExecutors.IO) ExecutorService ioPool;

	@AfterEach
	void releaseTheLock() {
		lockReleased.countDown();
		waitingClients.shutdown();
	}

	@ParameterizedTest
	@ValueSource(strings = {AvatarSummariesController.PATH, BANK_ROUTE, "/api/v1/avatars/" + AVATAR + "/storage", AdminStatusController.PATH, AccountsController.PATH})
	void readsTheDatabaseOnAPlatformThreadThatIsNoEventLoop(String route) {
		int status = statusWithALogin(route);

		assertThat(status).isEqualTo(OK.getCode());
		assertThat(reads).isNotEmpty().noneMatch(Read::onAnEventLoop).noneMatch(read -> read.thread().isVirtual());
	}

	@Test
	void readsTheAccountHashesThePasswordAndWritesTheLastLoginOnAPlatformThreadThatIsNoEventLoop() {
		int status = new RawHttpClient(server.getPort()).statusOfRequest(loginRequest());

		assertThat(status).isEqualTo(OK.getCode());
		assertThat(loginWork).extracting(Work::what).containsExactly("findByNameKey", "verify", "recordLastLogin");
		assertThat(loginWork).noneMatch(Work::onAnEventLoop).noneMatch(work -> work.thread().isVirtual());
	}

	@Test
	void answersAnotherRequestWhileAReadWaitsOnALock() {
		readsWaitOnALock = true;
		CompletableFuture<Integer> waitingRead = CompletableFuture.supplyAsync(() -> statusWithALogin(BANK_ROUTE), waitingClients);
		waitingRead.whenComplete((status, failure) -> readStarted.countDown());
		boolean theReadIsWaiting = silentThrow(() -> readStarted.await(HANG_GUARD_MILLIS, MILLISECONDS)) && !waitingRead.isDone();

		int statusMeanwhile = new RawHttpClient(server.getPort()).statusOf("/health");
		lockReleased.countDown();
		int statusOfTheWaitingRead = waitingRead.join();

		assertThat(theReadIsWaiting).isTrue();
		assertThat(statusMeanwhile).isEqualTo(OK.getCode());
		assertThat(statusOfTheWaitingRead).isEqualTo(OK.getCode());
	}

	@Test
	void queuesTheRequestsBeyondTheIoPoolInsteadOfStartingMoreThreads() {
		int poolSize = Objects.requireNonNull(ioConfiguration.getNumberOfThreads());
		ThreadPoolExecutor pool = (ThreadPoolExecutor) ContextPropagatingExecutorService.unwrap(ioPool).orElse(ioPool);
		readsWaitOnALock = true;
		List<CompletableFuture<Integer>> requests = IntStream
				.rangeClosed(0, poolSize)
				.mapToObj(_ -> CompletableFuture.supplyAsync(() -> statusWithALogin(BANK_ROUTE), waitingClients))
				.toList();

		boolean theRestQueues = eventually(() -> waitingReads.get() == poolSize && !pool.getQueue().isEmpty());
		lockReleased.countDown();
		List<String> outcomes = requests
				.stream()
				.map(request -> request.handle((status, failure) -> failure == null ? String.valueOf(status) : "no answer: " + failure.getCause()).join())
				.toList();

		assertThat(theRestQueues).isTrue();
		assertThat(outcomes).containsOnly(String.valueOf(OK.getCode()));
	}

	@Test
	void checksTheIssuedPasswordHashesTheNewOneAndWritesBothOnAPlatformThreadThatIsNoEventLoop() {
		String body = "{\"name\":\"" + ISSUED_AVATAR + "\",\"issuedPassword\":\"" + ISSUED_PASSWORD + "\",\"newPassword\":\"" + PASSWORD + "\"}";

		int status = new RawHttpClient(server.getPort()).statusOfRequest(postRequest("/api/v1/login/new-password", body));

		assertThat(status).isEqualTo(OK.getCode());
		assertThat(loginWork).extracting(Work::what).containsExactly("findByNameKey", "verify", "hash", "replaceIssuedPassword", "recordLastLogin");
		assertThat(loginWork).noneMatch(Work::onAnEventLoop).noneMatch(work -> work.thread().isVirtual());
	}

	@Test
	void checksTheCurrentPasswordHashesTheNewOneAndWritesItOnAPlatformThreadThatIsNoEventLoop() {
		String body = "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + CHANGED_PASSWORD + "\"}";
		String request = postRequest("/api/v1/password", body)
				.replace("Content-Type: application/json", "Content-Type: application/json\r\nCookie: " + LoginCookie.memberHeaderFor(server, AVATAR));

		int status = new RawHttpClient(server.getPort()).statusOfRequest(request);

		assertThat(status).isEqualTo(NO_CONTENT.getCode());
		assertThat(loginWork).extracting(Work::what).containsExactly("findByNameKey", "verify", "hash", "replacePassword");
		assertThat(loginWork).noneMatch(Work::onAnEventLoop).noneMatch(work -> work.thread().isVirtual());
	}

	@Test
	void readsTheAccountHashesTheIssuedPasswordAndWritesItOnAPlatformThreadThatIsNoEventLoop() {
		witnessEveryHash = true;
		String request = "POST " + AccountsController.PATH + "/" + AVATAR + "/reset HTTP/1.1\r\nHost: localhost\r\nCookie: " + LoginCookie.headerFor(server)
				+ "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";

		int status = new RawHttpClient(server.getPort()).statusOfRequest(request);

		assertThat(status).isEqualTo(OK.getCode());
		assertThat(loginWork).extracting(Work::what).containsExactly("findByNameKey", "hash", "replacePassword");
		assertThat(loginWork).noneMatch(Work::onAnEventLoop).noneMatch(work -> work.thread().isVirtual());
	}

	private int statusWithALogin(String route) {
		return new RawHttpClient(server.getPort())
				.statusOfRequest("GET " + route + " HTTP/1.1\r\nHost: localhost\r\nCookie: " + LoginCookie.headerFor(server) + "\r\nConnection: close\r\n\r\n");
	}

	private static String loginRequest() {
		return postRequest("/api/v1/login", "{\"name\":\"" + AVATAR + "\",\"password\":\"" + PASSWORD + "\"}");
	}

	private static String postRequest(String path, String body) {
		return "POST " + path + " HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: " + body.length() + "\r\nConnection: close\r\n\r\n" + body;
	}

	private static boolean eventually(BooleanSupplier condition) {
		long deadline = System.nanoTime() + MILLISECONDS.toNanos(HANG_GUARD_MILLIS);
		while (!condition.getAsBoolean()) {
			if (System.nanoTime() > deadline) {
				return false;
			}
			LockSupport.parkNanos(MILLISECONDS.toNanos(1));
		}
		return true;
	}

	private void witnessRead() {
		reads.add(new Read(Thread.currentThread(), ThreadExecutorMap.currentExecutor() != null));
		if (readsWaitOnALock) {
			waitingReads.incrementAndGet();
			readStarted.countDown();
			silentThrow(() -> lockReleased.await());
		}
	}

	private void witnessLoginWork(String what) {
		loginWork.add(new Work(what, Thread.currentThread(), ThreadExecutorMap.currentExecutor() != null));
	}

	private record Read(Thread thread, boolean onAnEventLoop) {}

	private record Work(String what, Thread thread, boolean onAnEventLoop) {}

	@MockBean(AccountRepository.class)
	AccountRepository accountRepository() {
		return new AccountRepository() {
			private final List<Account> accounts = List
					.of(new Account(AVATAR, "witnessed:" + PASSWORD, PasswordState.CHOSEN, Role.MEMBER, Optional.empty()),
							new Account(ISSUED_AVATAR, "witnessed:" + ISSUED_PASSWORD, PasswordState.ISSUED, Role.MEMBER, Optional.empty()));

			@Override
			public Optional<Account> findByNameKey(String nameKey) {
				witnessLoginWork("findByNameKey");
				return accounts.stream().filter(held -> held.nameKey().equals(nameKey)).findFirst();
			}

			@Override
			public List<Account> allAccounts() {
				return accounts;
			}

			@Override
			public boolean addIfAbsent(Account toAdd) {
				return false;
			}

			@Override
			public void replacePassword(String nameKey, String passwordHash, PasswordState state) {
				witnessLoginWork("replacePassword");
			}

			@Override
			public boolean replaceIssuedPassword(String nameKey, String issuedHash, String passwordHash) {
				witnessLoginWork("replaceIssuedPassword");
				return true;
			}

			@Override
			public void recordLastLogin(String nameKey, Instant at) {
				witnessLoginWork("recordLastLogin");
			}
		};
	}

	@MockBean(PasswordHasher.class)
	PasswordHasher passwordHasher() {
		return new PasswordHasher() {
			@Override
			public String hash(String clear) {
				if (witnessEveryHash || clear.equals(PASSWORD) || clear.equals(CHANGED_PASSWORD)) {
					witnessLoginWork("hash");
				}
				return "witnessed:" + clear;
			}

			@Override
			public boolean verify(String clear, String encoded) {
				witnessLoginWork("verify");
				return encoded.equals("witnessed:" + clear);
			}
		};
	}

	@MockBean(BankRepository.class)
	BankRepository bankRepository() {
		BankRepositoryStub bank = new BankRepositoryStub() {
			@Override
			public long countFor(String avatar) {
				witnessRead();
				return super.countFor(avatar);
			}

			@Override
			public List<BankEntry> getAllFor(String avatar, LedgerSort<BankSortKey> sort, long page, long size) {
				return getAllFor(avatar);
			}

			@Override
			public List<String> getAllDifferentAvatars() {
				witnessRead();
				return super.getAllDifferentAvatars();
			}

			@Override
			public Map<String, Instant> latestTimestampPerAvatar() {
				witnessRead();
				return super.latestTimestampPerAvatar();
			}
		};
		bank.seedAvatars(List.of(AVATAR));
		bank.seedEntries(AVATAR, List.of(new BankEntry(Instant.parse("2026-01-10T10:00:00Z"), AVATAR, 1000, EINLAGERUNG)));
		bank.seedEntries(ISSUED_AVATAR, List.of(new BankEntry(Instant.parse("2026-01-10T10:00:00Z"), ISSUED_AVATAR, 1, EINLAGERUNG)));
		return bank;
	}

	@MockBean(StorageRepository.class)
	StorageRepository storageRepository() {
		StorageRepositoryStub storage = new StorageRepositoryStub() {
			@Override
			public long countFor(String avatar) {
				witnessRead();
				return super.countFor(avatar);
			}

			@Override
			public List<StorageEntry> getAllFor(String avatar, LedgerSort<StorageSortKey> sort, long page, long size) {
				return getAllFor(avatar);
			}
		};
		storage.seedEntries(AVATAR, List.of(new StorageEntry(Instant.parse("2026-01-10T10:00:00Z"), AVATAR, 1, "Federn", 0, EINLAGERUNG)));
		return storage;
	}

	@MockBean(MetaInformationRepository.class)
	MetaInformationRepository metaInformationRepository() {
		return new FakeMetaInformationRepository() {
			@Override
			public MetaInformationSnapshot snapshot() {
				witnessRead();
				return super.snapshot();
			}
		};
	}

	@MockBean(Configuration.class)
	Configuration configurationMock() {
		return new TestConfiguration();
	}

	public static class TestConfiguration extends Configuration {
		@Override
		public String getDatabasePath() {
			return WORKING_DB.toString();
		}

		@Override
		public int getCollectorInitialDelaySeconds() {
			return 3600;
		}
	}

	@MockBean(EvergoreDataExtractor.class)
	TestEvergoreDataExtractor testEvergoreDataExtractor() {
		return new TestEvergoreDataExtractor();
	}

	public static class TestEvergoreDataExtractor extends EvergoreDataExtractor {
		public TestEvergoreDataExtractor() {
			super(null, null, null, null);
		}

		@Override
		public void loadData() {}
	}

	@MockBean(PreDatabaseConnectionHook.class)
	PreDatabaseConnectionHook databaseHook() {
		return () -> {};
	}

	@MockBean(PostCollectionHook.class)
	PostCollectionHook collectionHook() {
		return () -> {};
	}
}
