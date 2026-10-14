package dev.schoenberg.evergore.protocolParser.application;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContribution;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.GuildContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.WholeGoldContribution;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.WholeGoldShare;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.domain.CatalogLookup;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ExplanationExamplesTest {
	private static final Path EXAMPLES = Path.of("frontend/src/app/explanation/explanation.json");
	private static final String GUILD = "Gilde";
	private static final String DEPOSIT = "Einlagerung";
	private static final String MULTIPLY = "×";
	private static final String ADD = "+";
	private static final double EXACT = 1e-9;
	private static final double GUILD_PRICE = 60;

	@ParameterizedTest
	@MethodSource("answersWithCalculations")
	void pricesAndCreditsEveryMovementAsTheCatalogDoes(String question, Example example) {
		for (Line line : movementsOf(example)) {
			EvergoreItem item = CatalogLookup.itemFor(line.item()).orElseThrow(() -> new AssertionError("not in the catalog: " + line.item()));

			assertThat(line.price()).as("the price of %s", line.item()).isEqualTo(item.marketValue);
			assertThat(line.share()).as("the share of %s", line.item()).isEqualTo(shareOf(line, item));
			assertThat(line.marketStall()).as("whether %s is a deposit bought at the market stall", line.item()).isEqualTo(isADepositBoughtAtTheMarketStall(line, item));
			assertThat(line.result()).as("the worth of %s", line.item()).isCloseTo(line.quantity() * line.price() * line.share() / 100.0 * line.quality() / 100.0, within(EXACT));
		}
	}

	@ParameterizedTest
	@MethodSource("answersWithCalculations")
	void addsUpEveryCalculationOfAFigure(String question, Example example) {
		for (Line line : figuresOf(example)) {
			List<Double> terms = line.terms().stream().map(term -> valueOf(term, example)).toList();

			assertThat(leftToRight(terms, line.operators())).as("%s worked out from left to right", line.label()).isCloseTo(line.result(), within(EXACT));
			assertThat(multiplyingFirst(terms, line.operators())).as("%s worked out multiplying first", line.label()).isCloseTo(line.result(), within(EXACT));
		}
	}

	@ParameterizedTest
	@MethodSource("answersWithCalculations")
	void endsEveryFigureOnTheOneTheOverviewShows(String question, Example example) {
		GuildContributions guild = valued(example.ledger());

		for (Line line : figuresOf(example)) {
			assertThat(wholeGoldOf(line.result())).as("%s %s", line.row(), line.label()).isEqualTo(shownFigure(guild, line.row(), line.label()));
		}
	}

	static Stream<Arguments> answersWithCalculations() throws IOException {
		Explanation explanation = new ObjectMapper().readValue(EXAMPLES.toFile(), Explanation.class);
		return explanation
				.groups()
				.stream()
				.flatMap(group -> group.answers().stream())
				.filter(answer -> !answer.example().lines().isEmpty())
				.map(answer -> Arguments.of(answer.question(), answer.example()));
	}

	private static List<Line> movementsOf(Example example) {
		return example.lines().stream().filter(line -> line.kind().equals("movement")).toList();
	}

	private static List<Line> figuresOf(Example example) {
		return example.lines().stream().filter(line -> line.kind().equals("figure")).toList();
	}

	private static int shareOf(Line line, EvergoreItem item) {
		if (line.atGuildPrice() || !line.movement().equals(DEPOSIT)) {
			return (int) GUILD_PRICE;
		}
		return (int) Math.round(item.placement() * 100);
	}

	private static boolean isADepositBoughtAtTheMarketStall(Line line, EvergoreItem item) {
		return line.movement().equals(DEPOSIT) && item.placement() == 1.0;
	}

	private static double valueOf(Term term, Example example) {
		if (term.line() != null) {
			return example.lines().get(term.line()).result();
		}
		if (term.share() != null) {
			return term.share() / 100.0;
		}
		return term.amount();
	}

	private static double leftToRight(List<Double> terms, List<String> operators) {
		double value = terms.getFirst();
		for (int index = 0; index < operators.size(); index++) {
			value = apply(value, operators.get(index), terms.get(index + 1));
		}
		return value;
	}

	private static double multiplyingFirst(List<Double> terms, List<String> operators) {
		List<Double> sums = new ArrayList<>(List.of(terms.getFirst()));
		List<String> signs = new ArrayList<>();
		for (int index = 0; index < operators.size(); index++) {
			if (operators.get(index).equals(MULTIPLY)) {
				sums.set(sums.size() - 1, sums.getLast() * terms.get(index + 1));
			} else {
				signs.add(operators.get(index));
				sums.add(terms.get(index + 1));
			}
		}
		return leftToRight(sums, signs);
	}

	private static double apply(double value, String operator, double term) {
		if (operator.equals(MULTIPLY)) {
			return value * term;
		}
		return operator.equals(ADD) ? value + term : value - term;
	}

	private static GuildContributions valued(Ledger ledger) {
		BankRepositoryStub bankRepo = new BankRepositoryStub();
		StorageRepositoryStub storageRepo = new StorageRepositoryStub();
		FakeMetaInformationRepository metaRepo = new FakeMetaInformationRepository();
		Map<String, List<BankEntry>> bank = ledger
				.bank()
				.stream()
				.collect(Collectors.groupingBy(BankMovement::member, Collectors.mapping(BankMovement::entry, Collectors.toList())));
		Map<String, List<StorageEntry>> storage = ledger
				.storage()
				.stream()
				.collect(Collectors.groupingBy(StorageMovement::member, Collectors.mapping(StorageMovement::entry, Collectors.toList())));
		bank.forEach(bankRepo::seedEntries);
		storage.forEach(storageRepo::seedEntries);
		bankRepo.seedAvatars(List.copyOf(bank.keySet()));
		storageRepo.seedAvatars(List.copyOf(storage.keySet()));
		KnownAvatars knownAvatars = new KnownAvatars(bankRepo, storageRepo);

		new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, knownAvatars, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), new LoggerSpy()).evaluateData();
		return new AvatarContributions(knownAvatars, metaRepo, bankRepo, storageRepo).ofEveryKnownAvatar();
	}

	private static long shownFigure(GuildContributions guild, String row, String label) {
		WholeGoldContribution figures = row == null || row.equals(GUILD) ? guild.total().orElseThrow().inWholeGold() : wholeGoldOf(guild, row);
		WholeGoldShare share = figures.guildShare().orElseThrow();
		return switch (label) {
			case "Bank-Einzahlung" -> figures.bankDeposited();
			case "Bank-Auszahlung" -> figures.bankWithdrawn();
			case "Einlagerung" -> figures.storageDeposited();
			case "Entnahme" -> figures.storageWithdrawn();
			case "Nach Abzügen" -> figures.net();
			case "Vor Abzügen" -> share.balance();
			case "Gildenbank" -> figures.bankDeposited() - figures.bankWithdrawn();
			case "Gildenlagerwert" -> share.storageValue();
			case "Gildenspende" -> share.donation();
			case "Handwerkssubventionen" -> share.craftSubsidy();
			default -> throw new AssertionError("no figure of the overview is labelled " + label);
		};
	}

	private static WholeGoldContribution wholeGoldOf(GuildContributions guild, String member) {
		return guild
				.avatars()
				.stream()
				.filter(avatar -> avatar.avatar().equals(member))
				.findFirst()
				.flatMap(AvatarContribution::contribution)
				.orElseThrow(() -> new AssertionError("no figures for " + member))
				.inWholeGold();
	}

	private static long wholeGoldOf(double exact) {
		return exact < 0 ? -Math.round(-exact) : Math.round(exact);
	}

	private static TransferType transferOf(String movement) {
		return movement.equals(DEPOSIT) || movement.equals("Einzahlung") ? EINLAGERUNG : ENTNAHME;
	}

	record Explanation(List<Persona> personas, List<Group> groups) {}

	record Persona(String name, String sentence) {}

	record Group(String title, List<Answer> answers) {}

	record Answer(String question, String rule, List<String> points, Example example, String why) {}

	record Example(String intro, Ledger ledger, List<Line> lines) {}

	record Ledger(List<BankMovement> bank, List<StorageMovement> storage) {}

	record BankMovement(String member, int amount, String movement) {
		BankEntry entry() {
			return new BankEntry(Instant.EPOCH, member, amount, transferOf(movement));
		}
	}

	record StorageMovement(String member, int quantity, String item, int quality, String movement) {
		StorageEntry entry() {
			return new StorageEntry(Instant.EPOCH, member, quantity, item, quality, transferOf(movement));
		}
	}

	record Line(String kind, String member, String movement, int quantity, String item, int quality, boolean marketStall, boolean atGuildPrice, int price, int share, String row,
			String label, List<Term> terms, List<String> operators, double result) {}

	record Term(Integer line, Double amount, Integer share) {}
}
