package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.ExplanationPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ExplanationPage.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.TokenChoice;
import dev.schoenberg.evergore.protocolParser.acceptance.steps.Calculation.Moved;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Ingredient;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Recipe.Published;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.EXPLANATION;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.Category.HANDWERKSMATERIAL;
import static org.assertj.core.api.Assertions.assertThat;

public class ExplanationSteps {
	private static final String START_PAGE = "/";
	private static final String LINK_FROM_THE_OVERVIEW = "Wie entstehen diese Zahlen?";
	private static final String HANNA = "Hanna Handwerk";
	private static final String GERO = "Gero Gildenhändler";
	private static final String GUILD = "Gilde";
	private static final List<String> INVENTED_MEMBERS = List.of("Mira Mitglied", HANNA, GERO);
	private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");
	private static final Pattern PAID_IN = Pattern.compile("zahlt (\\d{1,3}(?:\\.\\d{3})* Gold) ein");
	private static final Pattern TAKEN_OUT = Pattern.compile("hebt (\\d{1,3}(?:\\.\\d{3})* Gold) ab");
	private static final Pattern NO_LETTER = Pattern.compile("[^\\p{L}]+");
	private static final String AMMUNITION = "MUNITION_";
	private static final Set<String> NO_EQUIPMENT = Set
			.of("ROHSTOFFE", "JAGDBEUTEN", "EDELSTEINE", "HANDWERKSMATERIAL", "VERARBEITETE_ROHSTOFFE", "BANDAGEN", "BAUTEILE", "MUNITION_BOEGEN", "MUNITION_ARMBRUESTE",
					"MUNITION_MAGIESTAEBE");
	private static final double GUILD_PRICE = 0.6;
	private static final double FULL_CREDIT = 1.0;

	private final MemberBrowser browser;

	public ExplanationSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens the explanation page")
	public void aMemberOpensTheExplanationPage() {
		browser.open(EXPLANATION);
	}

	@When("a member opens a bookmark of the explanation page {tokenChoice}")
	public void aMemberOpensABookmarkOfTheExplanationPage(TokenChoice choice) {
		browser.openADeepLinkBookmark(EXPLANATION, choice);
	}

	@When("a member follows the guild's link to the explanation page")
	public void aMemberFollowsTheGuildsLinkToTheExplanationPage() {
		browser.open(START_PAGE);
		browser.follow(LINK_FROM_THE_OVERVIEW);
	}

	@When("the member follows {string}")
	public void theMemberFollows(String linkText) {
		browser.follow(linkText);
	}

	@Then("the page asks exactly these questions, in this order, under these groups:")
	public void thePageAsksExactlyTheseQuestions(DataTable expected) {
		List<List<String>> asked = page().groups().stream().flatMap(group -> group.answers().stream().map(answer -> List.of(group.title(), answer.question()))).toList();

		assertThat(asked).isEqualTo(expected.cells().subList(1, expected.height()));
	}

	@Then("each group's title is a heading of the page, and each of its questions a heading one level below it")
	public void eachGroupTitleIsAHeadingAboveItsQuestions() {
		assertThat(page().groups()).isNotEmpty().allSatisfy(group -> {
			assertThat(group.titleLevel()).as("the heading level of %s", group.title()).isPositive();
			assertThat(group.answers()).allSatisfy(answer -> assertThat(answer.questionLevel()).as("the heading level of %s", answer.question()).isEqualTo(group.titleLevel() + 1));
		});
	}

	@Then("the page says, above its first group:")
	public void thePageSaysAboveItsFirstGroup(DataTable expected) {
		ExplanationPage page = page();

		assertThat(page.legendAboveTheGroups()).as("the legend stands above the first group").isTrue();
		for (List<String> row : expected.cells().subList(1, expected.height())) {
			List<String> sentences = page.legend().stream().flatMap(SENTENCE_END::splitAsStream).toList();
			assertThat(sentences).as("a sentence naming %s as %s", row.get(0), row.get(1)).anyMatch(sentence -> sentence.contains(row.get(0)) && sentence.contains(row.get(1)));
		}
	}

	@Then("each of the {int} questions on the page has an answer whose rule stands before one example that opens with {string} and names {string}, {string} or {string}")
	public void eachQuestionHasItsRuleBeforeOneExample(int count, String opener, String first, String second, String third) {
		List<Answer> answers = page().answers();

		assertThat(answers).hasSize(count);
		assertThat(answers).allSatisfy(answer -> {
			assertThat(answer.ruleBeforeExample()).as("the rule of %s stands before its example", answer.question()).isTrue();
			assertThat(answer.examples()).as("the examples of %s", answer.question()).isEqualTo(1);
			assertThat(answer.example()).startsWith(opener).containsAnyOf(first, second, third);
		});
	}

	@Then("the page does not say {string}")
	public void thePageDoesNotSay(String words) {
		assertThat(page().text()).doesNotContain(words);
	}

	@Then("the rule in the answer to {string} says {string}")
	public void theRuleInTheAnswerSays(String question, String words) {
		assertThat(spaced(page().answerTo(question).rule())).contains(spaced(words));
	}

	@Then("the answer to {string} lists exactly these points:")
	public void theAnswerListsExactlyThesePoints(String question, DataTable expected) {
		List<String> points = page().answerTo(question).points().stream().map(ExplanationSteps::spaced).toList();
		List<String> wanted = expected.column(0).subList(1, expected.height());

		assertThat(points).hasSameSizeAs(wanted);
		for (String point : wanted) {
			assertThat(points).as("a point saying %s", point).filteredOn(shown -> shown.contains(spaced(point))).hasSize(1);
		}
	}

	@Then("every calculation of the page writes an amount with {string} after it in German digit grouping and a share with {string} after it, and adds up whether worked out from left to right or with {string} before {string} and {string}")
	public void everyCalculationIsWrittenAsTheOverviewWritesFigures(String gold, String share, String times, String plus, String minus) {
		List<Calculation> calculations = allCalculations();

		assertThat(calculations).isNotEmpty();
		assertThat(calculations).allSatisfy(calculation -> {
			assertThat(calculation.isWrittenAsTheOverviewWritesFigures()).as("%s is written as the overview writes figures", calculation.opener()).isTrue();
			assertThat(calculation.addsUp()).as("%s adds up", calculation.opener()).isTrue();
		});
		assertThat(List.of(gold, share, times, plus, minus)).containsExactly("Gold", "%", "×", "+", "−");
	}

	@Then("every calculation of the page writes a result that is no whole gold with an arrow and its nearest whole gold after it, and any other result without an arrow")
	public void everyCalculationShowsItsResultAsTheOverviewShowsIt() {
		List<Calculation> calculations = allCalculations();

		assertThat(calculations)
				.isNotEmpty()
				.allSatisfy(calculation -> assertThat(calculation.showsItsResultAsTheOverviewShowsIt()).as("the result of %s", calculation.opener()).isTrue());
	}

	@Then("every calculation of the page writes a term below zero in parentheses, with the short minus sign, and no other term in parentheses")
	public void everyCalculationBracketsItsTermsBelowZero() {
		assertThat(allCalculations())
				.isNotEmpty()
				.allSatisfy(calculation -> assertThat(calculation.bracketsExactlyItsTermsBelowZero()).as("the terms of %s", calculation.opener()).isTrue());
	}

	@Then("every calculation of the page opens with a movement or with one of these figures, alone or after \"Gilde\" or the name of an invented member:")
	public void everyCalculationOpensWithAMovementOrAFigure(DataTable figures) {
		List<String> labels = figures.column(0).subList(1, figures.height());
		Predicate<Calculation> opensWell = calculation -> calculation.movement().isPresent() || opensWithAFigure(calculation.opener(), labels);

		assertThat(allCalculations()).isNotEmpty().allSatisfy(calculation -> assertThat(opensWell.test(calculation)).as("the opening of %s", calculation.opener()).isTrue());
	}

	@Then("the example of {string} works out, each in a calculation of its own, a deposit of goods and a withdrawal of goods by {word} {word}")
	public void worksOutADepositAndAWithdrawalOfGoods(String question, String name, String surname) {
		assertThat(depositAndWithdrawalOf(question, name + " " + surname)).hasSize(2);
	}

	@Then("the example of {string} works out a calculation that ends on the {string} figure of {word} {word}, whose terms are the gold she paid in and the result of the calculation of her deposit of goods, both added, and the gold she took out and the result of the calculation of her withdrawal of goods, both taken off")
	public void worksOutTheFigureFromGoldAndGoods(String question, String figure, String name, String surname) {
		String intro = page().answerTo(question).exampleIntro();
		List<Calculation> pair = depositAndWithdrawalOf(question, name + " " + surname);
		Calculation calculation = endingOn(calculationsOf(question), name + " " + surname, figure);
		List<Double> added = new ArrayList<>();
		List<Double> takenOff = new ArrayList<>();
		for (int index = 0; index < calculation.terms().size(); index++) {
			(calculation.signedTerm(index) < 0 ? takenOff : added).add(calculation.terms().get(index));
		}

		assertThat(added).containsExactlyInAnyOrder(goldIn(intro, PAID_IN), pair.getFirst().exact());
		assertThat(takenOff).containsExactlyInAnyOrder(goldIn(intro, TAKEN_OUT), pair.getLast().exact());
	}

	@Then("the example of {string} works out, in a calculation of its own, {word} {word}'s deposit of goods bought at the market stall, at the guild's price")
	public void worksOutABoughtDepositAtTheGuildPrice(String question, String name, String surname) {
		assertThat(boughtDepositAtTheGuildPrice(question, name + " " + surname)).isNotNull();
	}

	@Then("the example of {string} works out a calculation of at least two terms that ends on the {string} figure of {word} {word}, one term the result of the calculation of her deposit bought at the market stall at the guild's price, each term a share, an amount the example names or the result of another of its calculations")
	public void worksOutTheFigureFromTheBoughtDeposit(String question, String figure, String name, String surname) {
		Calculation deposit = boughtDepositAtTheGuildPrice(question, name + " " + surname);
		List<Calculation> calculations = calculationsOf(question);
		Calculation calculation = endingOn(calculations, name + " " + surname, figure);

		assertThat(calculation.terms()).hasSizeGreaterThanOrEqualTo(2).anyMatch(term -> Math.abs(term - deposit.exact()) < 1e-9);
		assertThat(termsComeFromTheExample(calculation, calculations, page().answerTo(question).exampleIntro())).isTrue();
	}

	@Then("the example of {string} works out a calculation of at least two terms that ends on a {string} figure, each term a share, an amount the example names or the result of another of its calculations")
	public void worksOutACalculationFromItsOwnMovements(String question, String figure) {
		Answer answer = page().answerTo(question);
		List<Calculation> calculations = calculationsOf(question);

		assertThat(calculations)
				.anyMatch(
						calculation -> calculation.endsOn(figure) && calculation.terms().size() >= 2 && termsComeFromTheExample(calculation, calculations, answer.exampleIntro()));
	}

	@Then("the example of {string} works out a calculation of at least two terms that ends on the {string} figure of a member and one that ends on the {string} figure of the same member, which differs from it, each term of both a share, an amount the example names or the result of another of its calculations")
	public void worksOutOneMembersFiguresAfterAndBefore(String question, String after, String before) {
		String intro = page().answerTo(question).exampleIntro();
		List<Calculation> calculations = calculationsOf(question);
		Predicate<Calculation> fromTheExample = calculation -> calculation.terms().size() >= 2 && termsComeFromTheExample(calculation, calculations, intro);
		Predicate<Calculation> hasItsFigureBefore = calculation -> calculations
				.stream()
				.anyMatch(other -> isTheFigureOfTheSameRow(other, before, calculation, after) && fromTheExample.test(other));

		assertThat(calculations).filteredOn(calculation -> calculation.endsOn(after) && fromTheExample.test(calculation)).anyMatch(hasItsFigureBefore);
	}

	@Then("the example of {string} works out these six deposits, each in a different calculation of its own:")
	public void worksOutTheseSixDeposits(String question, DataTable expected) {
		List<Moved> deposits = new ArrayList<>(movementsOf(question).stream().filter(Moved::deposit).toList());

		for (String kind : expected.column(0).subList(1, expected.height())) {
			Moved match = deposits.stream().filter(depositOfTheKind(kind)).findFirst().orElseThrow(() -> new AssertionError("no calculation of a deposit " + kind));
			deposits.remove(match);
		}
	}

	@Then("the example of {string} works out, each in a calculation of its own, a withdrawal of equipment of quality 100 and one of the same equipment of quality 50, which gives half the result of the other")
	public void worksOutTheSameEquipmentAtTwoQualities(String question) {
		List<Moved> withdrawals = movementsOf(question).stream().filter(moved -> !moved.deposit() && isEquipment(moved.catalogItem())).toList();

		assertThat(withdrawals)
				.anySatisfy(full -> assertThat(withdrawals)
						.anyMatch(half -> full.quality() == 100 && half.quality() == 50 && full.item().equals(half.item()) && full.quantity() == half.quantity()
								&& Math.abs(half.result() * 2 - full.result()) < 1e-9));
	}

	@Then("the example of {string} works out a calculation that ends on the {string} figure of the row {string}, whose terms are the results of the calculations that end on the {string} figures of {word} {word} and {word} {word}")
	public void worksOutTheGuildRowFromTheMembers(String question, String figure, String row, String membersFigure, String firstName, String firstSurname, String secondName,
			String secondSurname) {
		List<Calculation> calculations = calculationsOf(question);
		Calculation first = endingOn(calculations, firstName + " " + firstSurname, membersFigure);
		Calculation second = endingOn(calculations, secondName + " " + secondSurname, membersFigure);
		List<Calculation> members = List.of(first, second);

		assertThat(endingOn(calculations, row, figure).takesAsTerms(members)).isTrue();
	}

	@Then("the example of {string} works out a calculation that ends on a {string} figure, has a term that is the exact result of another calculation which shows that result with an arrow and its nearest whole gold, and whose figure is 1 Gold more or less than its terms give when each term that is the result of another calculation is taken as the whole gold that calculation shows")
	public void worksOutAFigureOneGoldOffItsShownTerms(String question, String figure) {
		List<Calculation> example = calculationsOf(question);

		assertThat(example)
				.anyMatch(calculation -> calculation.endsOn(figure) && takesARoundedResult(calculation, example)
						&& Math.abs(calculation.fromTheShownFiguresOf(example) - calculation.shown()) == 1);
	}

	@Then("neither the rule nor the example in the answer to {string} has a word that begins with {string} or {string}, in upper or lower case")
	public void neitherTheRuleNorTheExampleHasAWordBeginningWith(String question, String first, String second) {
		Answer answer = page().answerTo(question);
		List<String> beginnings = List.of(first.toLowerCase(Locale.GERMAN), second.toLowerCase(Locale.GERMAN));

		assertThat(NO_LETTER.splitAsStream(answer.rule() + " " + answer.example()).map(word -> word.toLowerCase(Locale.GERMAN)))
				.noneMatch(word -> beginnings.stream().anyMatch(word::startsWith));
	}

	@Then("the example of {string} works out, each in a calculation of its own, Hanna Handwerk's withdrawals of the ingredients and her deposit of the product")
	public void worksOutTheCrafting(String question) {
		assertThat(craftingOf(question)).isNotEmpty();
	}

	@Then("the example of {string} works out a calculation that ends on the {string} figure of Hanna Handwerk, whose terms are the result of the calculation of her deposit of the product less the results of the calculations of her withdrawals of the ingredients")
	public void worksOutTheCraftingGain(String question, String figure) {
		List<Calculation> crafting = craftingOf(question);
		Calculation gain = endingOn(calculationsOf(question), HANNA, figure);

		assertThat(gain.takesAsTerms(crafting)).isTrue();
		assertThat(gain.terms().getFirst()).isEqualTo(crafting.getFirst().exact());
		assertThat(gain.operators()).containsOnly("−");
	}

	@Then("the example of {string} works out, each in a calculation of its own, a deposit credited {word} the guild's price and the same deposit at the guild's price")
	public void worksOutADepositAndItsGuildPrice(String question, String side) {
		assertThat(depositAndItsGuildPrice(question, side)).hasSize(2);
	}

	@Then("the example of {string} works out a calculation that ends on a {string} figure, whose terms are the result of the calculation of the deposit at the guild's price less the result of the calculation of the deposit credited below it")
	public void worksOutTheDonation(String question, String figure) {
		List<Calculation> pair = depositAndItsGuildPrice(question, "below");

		assertThat(calculationsOf(question)).anyMatch(calculation -> calculation.endsOn(figure) && isFirstLessSecond(calculation, pair.getLast(), pair.getFirst()));
	}

	@Then("the example of {string} works out a calculation that ends on a {string} figure, whose terms are the result of the calculation of the deposit credited above it less the result of the calculation of the deposit at the guild's price")
	public void worksOutTheCraftSubsidy(String question, String figure) {
		List<Calculation> pair = depositAndItsGuildPrice(question, "above");

		assertThat(calculationsOf(question)).anyMatch(calculation -> calculation.endsOn(figure) && isFirstLessSecond(calculation, pair.getFirst(), pair.getLast()));
	}

	@Then("the example of {string} works out, in a calculation of its own, a withdrawal of goods from the guild storage by Gero Gildenhändler")
	public void worksOutGerosWithdrawal(String question) {
		assertThat(gerosWithdrawal(question)).isNotNull();
	}

	@Then("the example of {string} works out, each in a calculation of its own, a deposit of crafting material bought at the market stall by Gero Gildenhändler, credited in full, and the same deposit at the guild's price")
	public void worksOutGerosDeposits(String question) {
		Moved inFull = gerosDeposit(question, FULL_CREDIT).movement().orElseThrow();
		Moved atTheGuildPrice = gerosDeposit(question, GUILD_PRICE).movement().orElseThrow();

		assertThat(atTheGuildPrice.isTheSameMovementAs(inFull)).as("the same deposit at the guild's price").isTrue();
	}

	@Then("the example of {string} works out a calculation that ends on the {string} figure of Gero Gildenhändler, above zero, whose terms are the result of the calculation of his deposit credited in full less the result of the calculation of his withdrawal of goods from the guild storage")
	public void worksOutGerosFigureAfterTheDeductions(String question, String figure) {
		Calculation calculation = endingOn(calculationsOf(question), GERO, figure);

		assertThat(calculation.shown()).isPositive();
		assertThat(isFirstLessSecond(calculation, gerosDeposit(question, FULL_CREDIT), gerosWithdrawal(question))).isTrue();
	}

	@Then("the example of {string} works out a calculation that ends on the {string} figure of Gero Gildenhändler, below zero, whose terms are the result of the calculation of his deposit at the guild's price less the result of the calculation of his withdrawal of goods from the guild storage")
	public void worksOutGerosFigureBeforeTheDeductions(String question, String figure) {
		Calculation calculation = endingOn(calculationsOf(question), GERO, figure);

		assertThat(calculation.shown()).isNegative();
		assertThat(isFirstLessSecond(calculation, gerosDeposit(question, GUILD_PRICE), gerosWithdrawal(question))).isTrue();
	}

	private ExplanationPage page() {
		return browser.read("read-explanation.js", ExplanationPage.class);
	}

	private List<Calculation> allCalculations() {
		return page().answers().stream().flatMap(answer -> answer.calculations().stream()).map(Calculation::of).toList();
	}

	private List<Calculation> depositAndWithdrawalOf(String question, String member) {
		List<Calculation> calculations = calculationsOf(question);
		Calculation deposit = theOne(calculations, moved -> moved.deposit() && moved.member().equals(member), "no deposit of goods by " + member);
		Calculation withdrawal = theOne(calculations, moved -> !moved.deposit() && moved.member().equals(member), "no withdrawal of goods by " + member);
		return List.of(deposit, withdrawal);
	}

	private Calculation boughtDepositAtTheGuildPrice(String question, String member) {
		return theOne(calculationsOf(question), moved -> moved.deposit() && moved.member().equals(member) && moved.boughtAtTheMarketStall() && moved.atTheGuildPrice(),
				"no deposit of goods bought at the market stall, at the guild's price, by " + member);
	}

	private static double goldIn(String intro, Pattern clause) {
		Matcher matcher = clause.matcher(intro);
		if (!matcher.find()) {
			throw new AssertionError("the example writes no gold in " + clause.pattern() + ": " + intro);
		}
		return Calculation.amountOf(matcher.group(1));
	}

	private List<Calculation> calculationsOf(String question) {
		List<Calculation> calculations = page().answerTo(question).calculations().stream().map(Calculation::of).toList();
		assertThat(calculations).as("every calculation of %s adds up", question).allMatch(Calculation::addsUp);
		return calculations;
	}

	private List<Moved> movementsOf(String question) {
		return calculationsOf(question).stream().flatMap(calculation -> calculation.movement().stream()).toList();
	}

	private List<Calculation> craftingOf(String question) {
		List<Calculation> calculations = calculationsOf(question);
		Calculation product = theOne(calculations, moved -> moved.deposit() && moved.member().equals(HANNA) && moved.catalogItem().recipe instanceof Published,
				"no deposit of a product by " + HANNA);
		Published recipe = (Published) product.movement().orElseThrow().catalogItem().recipe;
		List<Calculation> crafting = new ArrayList<>(List.of(product));
		int batches = product.movement().orElseThrow().quantity() / recipe.amount;
		for (Ingredient ingredient : recipe.ingredients) {
			int quantity = ingredient.amount * batches;
			Predicate<Moved> withdrawn = moved -> !moved.deposit() && moved.member().equals(HANNA) && moved.catalogItem() == ingredient.item && moved.quantity() == quantity;
			crafting.add(theOne(calculations, withdrawn, "no withdrawal of the ingredient " + ingredient.item.ingameName));
		}
		return crafting;
	}

	private List<Calculation> depositAndItsGuildPrice(String question, String side) {
		List<Calculation> calculations = calculationsOf(question);
		Predicate<Moved> credited = side.equals("below") ? Moved::belowTheGuildPrice : Moved::aboveTheGuildPrice;
		for (Calculation deposit : calculations) {
			if (deposit.movement().filter(credited).isEmpty()) {
				continue;
			}
			Moved moved = deposit.movement().orElseThrow();
			for (Calculation atGuildPrice : calculations) {
				if (atGuildPrice.movement().filter(other -> other.atTheGuildPrice() && other.isTheSameMovementAs(moved)).isPresent()) {
					return List.of(deposit, atGuildPrice);
				}
			}
		}
		return List.of();
	}

	private Calculation gerosWithdrawal(String question) {
		return theOne(calculationsOf(question), moved -> !moved.deposit() && moved.member().equals(GERO), "no withdrawal by " + GERO);
	}

	private Calculation gerosDeposit(String question, double share) {
		return theOne(calculationsOf(question), moved -> isGerosDepositOfCraftingMaterial(moved) && moved.share() == share && moved.atTheGuildPrice() == (share == GUILD_PRICE),
				"no deposit by " + GERO + " at " + share);
	}

	private static boolean isGerosDepositOfCraftingMaterial(Moved moved) {
		return moved.deposit() && moved.member().equals(GERO) && moved.boughtAtTheMarketStall() && moved.catalogItem().category == HANDWERKSMATERIAL;
	}

	private static Calculation theOne(List<Calculation> calculations, Predicate<Moved> wanted, String missing) {
		return calculations.stream().filter(calculation -> calculation.movement().filter(wanted).isPresent()).findFirst().orElseThrow(() -> new AssertionError(missing));
	}

	private static boolean termsComeFromTheExample(Calculation calculation, List<Calculation> calculations, String intro) {
		for (int index = 0; index < calculation.terms().size(); index++) {
			double term = calculation.terms().get(index);
			boolean named = namesTheWholeAmount(intro, calculation.writtenTerm(index).replace("(", "").replace(")", ""));
			boolean result = calculations.stream().anyMatch(other -> other != calculation && Math.abs(other.exact() - term) < 1e-9);
			if (!calculation.isAShare(index) && !named && !result) {
				return false;
			}
		}
		return true;
	}

	private static boolean opensWithAFigure(String opener, List<String> labels) {
		List<String> rows = new ArrayList<>(INVENTED_MEMBERS);
		rows.add(GUILD);
		return labels.stream().anyMatch(label -> opener.equals(label) || rows.stream().anyMatch(row -> opener.equals(row + " " + label)));
	}

	private static boolean namesTheWholeAmount(String text, String amount) {
		return Pattern.compile("(?<![\\d.,-])" + Pattern.quote(amount)).matcher(text).find();
	}

	private static boolean takesARoundedResult(Calculation calculation, List<Calculation> calculations) {
		return calculations
				.stream()
				.filter(other -> other != calculation && other.isNoWholeGoldShownAsTheNearest())
				.anyMatch(rounded -> calculation.terms().stream().anyMatch(term -> Math.abs(term - rounded.exact()) < 1e-9));
	}

	private static boolean isFirstLessSecond(Calculation calculation, Calculation first, Calculation second) {
		return calculation.terms().size() == 2 && calculation.operators().equals(List.of("−")) && Math.abs(calculation.terms().getFirst() - first.exact()) < 1e-9
				&& Math.abs(calculation.terms().getLast() - second.exact()) < 1e-9;
	}

	private static boolean isTheFigureOfTheSameRow(Calculation other, String otherFigure, Calculation calculation, String figure) {
		return other.endsOn(otherFigure) && other.terms().size() >= 2 && other.opener().replace(otherFigure, "").equals(calculation.opener().replace(figure, ""))
				&& other.shown() != calculation.shown();
	}

	private static Calculation endingOn(List<Calculation> calculations, String row, String figure) {
		return calculations
				.stream()
				.filter(calculation -> calculation.endsOnTheFigureOf(row, figure))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no calculation ending on " + row + " " + figure));
	}

	private static Predicate<Moved> depositOfTheKind(String kind) {
		return switch (kind) {
			case "credited at 0 % of its market value" -> moved -> moved.creditedAt(0) && !moved.boughtAtTheMarketStall();
			case "goods other than ammunition credited at 60 % of their market value, not scaled by a quality" ->
				moved -> moved.creditedAt(GUILD_PRICE) && moved.quality() == 100 && moved.terms() == 3 && !isAmmunition(moved.catalogItem());
			case "crafting material bought at the market stall" ->
				moved -> moved.boughtAtTheMarketStall() && moved.catalogItem().category == HANDWERKSMATERIAL && moved.creditedAt(FULL_CREDIT);
			case "ammunition the market stall sells, bought there" -> moved -> moved.boughtAtTheMarketStall() && isAmmunition(moved.catalogItem()) && moved.creditedAt(FULL_CREDIT);
			case "ammunition the market stall does not sell, credited at 60 % of its market value" ->
				moved -> isAmmunition(moved.catalogItem()) && moved.catalogItem().placement() < FULL_CREDIT && moved.creditedAt(GUILD_PRICE);
			case "equipment of a quality below 100, credited at 60 % with its quality as the last share" -> moved -> isEquipment(moved.catalogItem()) && moved.quality() < 100
					&& moved.creditedAt(GUILD_PRICE) && moved.terms() == 4 && moved.lastTerm() == moved.quality() / 100.0;
			default -> throw new AssertionError("no such kind of deposit: " + kind);
		};
	}

	private static boolean isAmmunition(EvergoreItem item) {
		return item.category.name().startsWith(AMMUNITION);
	}

	private static boolean isEquipment(EvergoreItem item) {
		return !NO_EQUIPMENT.contains(item.category.name());
	}

	private static String spaced(String text) {
		return text.replaceAll("[\\s\\u00A0]+", " ").strip();
	}
}
