package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.schoenberg.evergore.protocolParser.domain.CatalogLookup;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

record Calculation(String opener, List<String> writtenTerms, List<Double> terms, List<String> operators, String writtenResult, double exact, long shown, boolean roundedForShow) {
	private static final Pattern LINE = Pattern.compile("(?<opener>.+?): (?<expression>.+) = (?<result>[^=]+)");
	private static final Pattern MOVEMENT = Pattern
			.compile(
					"(?<member>.+?) (?<verb>legt|nimmt) (?<quantity>\\d+) (?<item>.+?)(?: der Qualität (?<quality>\\d+))?(?<stall> vom Marktstand)? (?:ein|heraus)(?<guildPrice>, zum Gildenpreis)?");
	private static final Pattern OPERATOR = Pattern.compile(" ([×+−]) ");
	private static final String ARROW = " → ";
	private static final double EXACT = 1e-9;
	private static final int FULL_QUALITY = 100;
	private static final double GUILD_PRICE = 0.6;
	private static final Pattern GOLD = Pattern.compile("-?\\d{1,3}(?:\\.\\d{3})*(?:,\\d+)? Gold");
	private static final Pattern WRITTEN_GOLD = Pattern.compile("\\(?" + GOLD.pattern() + "\\)?");
	private static final Pattern WRITTEN_SHARE = Pattern.compile("\\d+ %");
	private static final Pattern WRITTEN_RESULT = Pattern.compile(GOLD.pattern() + "(?: → " + GOLD.pattern() + ")?");
	private static final Pattern QUANTITY = Pattern.compile("\\d+");

	static Calculation of(String text) {
		Matcher line = LINE.matcher(text);
		if (!line.matches()) {
			throw new AssertionError("Not a calculation: " + text);
		}
		List<String> written = new ArrayList<>();
		List<String> operators = new ArrayList<>();
		Matcher operator = OPERATOR.matcher(line.group("expression"));
		int start = 0;
		while (operator.find()) {
			written.add(line.group("expression").substring(start, operator.start()));
			operators.add(operator.group(1));
			start = operator.end();
		}
		written.add(line.group("expression").substring(start));
		List<Double> terms = written.stream().map(Calculation::amountOf).toList();
		String result = line.group("result");
		boolean rounded = result.contains(ARROW);
		double exact = amountOf(rounded ? result.substring(0, result.indexOf(ARROW)) : result);
		long shown = rounded ? (long) amountOf(result.substring(result.indexOf(ARROW) + ARROW.length())) : Math.round(exact);
		return new Calculation(line.group("opener"), List.copyOf(written), terms, operators, result, exact, shown, rounded);
	}

	boolean isWrittenAsTheOverviewWritesFigures() {
		boolean movement = movement().isPresent();
		for (int index = 0; index < writtenTerms.size(); index++) {
			String term = writtenTerms.get(index);
			boolean quantity = movement && index == 0 && QUANTITY.matcher(term).matches();
			if (!quantity && !WRITTEN_GOLD.matcher(term).matches() && !WRITTEN_SHARE.matcher(term).matches()) {
				return false;
			}
		}
		return WRITTEN_RESULT.matcher(writtenResult).matches();
	}

	boolean isAShare(int index) {
		return WRITTEN_SHARE.matcher(writtenTerms.get(index)).matches();
	}

	String writtenTerm(int index) {
		return writtenTerms.get(index);
	}

	boolean addsUp() {
		return Math.abs(leftToRight(terms) - exact) < EXACT && Math.abs(multiplyingFirst(terms) - exact) < EXACT;
	}

	boolean endsOn(String figure) {
		return opener.equals(figure) || opener.endsWith(" " + figure);
	}

	boolean endsOnTheFigureOf(String row, String figure) {
		return opener.equals(row + " " + figure);
	}

	boolean takesAsTerms(List<Calculation> others) {
		List<Double> wanted = others.stream().map(Calculation::exact).sorted().toList();
		List<Double> given = terms.stream().sorted().toList();
		return given.size() == wanted.size() && matchesEach(given, wanted);
	}

	double fromTheShownFiguresOf(List<Calculation> example) {
		List<Double> shownTerms = terms
				.stream()
				.map(term -> example.stream().filter(other -> other != this && Math.abs(other.exact - term) < EXACT).findFirst().map(other -> (double) other.shown).orElse(term))
				.toList();
		return multiplyingFirst(shownTerms);
	}

	boolean bracketsExactlyItsTermsBelowZero() {
		for (int index = 0; index < writtenTerms.size(); index++) {
			String term = writtenTerms.get(index);
			boolean bracketed = term.startsWith("(") && term.endsWith(")");
			if (bracketed != terms.get(index) < 0 || term.contains("−")) {
				return false;
			}
		}
		return true;
	}

	double signedTerm(int index) {
		return index == 0 || operators.get(index - 1).equals("+") ? terms.get(index) : -terms.get(index);
	}

	boolean showsItsResultAsTheOverviewShowsIt() {
		return Math.abs(exact - Math.rint(exact)) < EXACT ? !roundedForShow : isNoWholeGoldShownAsTheNearest();
	}

	boolean isNoWholeGoldShownAsTheNearest() {
		return roundedForShow && Math.abs(exact - Math.rint(exact)) > EXACT && shown == (exact < 0 ? -Math.round(-exact) : Math.round(exact));
	}

	Optional<Moved> movement() {
		Matcher moved = MOVEMENT.matcher(opener);
		if (!moved.matches()) {
			return Optional.empty();
		}
		String quality = moved.group("quality");
		return Optional
				.of(new Moved(moved.group("member"), moved.group("verb").equals("legt"), Integer.parseInt(moved.group("quantity")), moved.group("item"),
						quality == null ? FULL_QUALITY : Integer.parseInt(quality), moved.group("stall") != null, moved.group("guildPrice") != null, shareOf(terms), terms.size(),
						terms.getLast(), exact));
	}

	private static double shareOf(List<Double> terms) {
		return terms.size() > 2 ? terms.get(2) : Double.NaN;
	}

	private static boolean matchesEach(List<Double> given, List<Double> wanted) {
		for (int index = 0; index < given.size(); index++) {
			if (Math.abs(given.get(index) - wanted.get(index)) > EXACT) {
				return false;
			}
		}
		return true;
	}

	private double leftToRight(List<Double> values) {
		double value = values.getFirst();
		for (int index = 0; index < operators.size(); index++) {
			value = apply(value, operators.get(index), values.get(index + 1));
		}
		return value;
	}

	private double multiplyingFirst(List<Double> values) {
		List<Double> sums = new ArrayList<>(List.of(values.getFirst()));
		List<String> signs = new ArrayList<>();
		for (int index = 0; index < operators.size(); index++) {
			if (operators.get(index).equals("×")) {
				sums.set(sums.size() - 1, sums.getLast() * values.get(index + 1));
			} else {
				signs.add(operators.get(index));
				sums.add(values.get(index + 1));
			}
		}
		double value = sums.getFirst();
		for (int index = 0; index < signs.size(); index++) {
			value = apply(value, signs.get(index), sums.get(index + 1));
		}
		return value;
	}

	private static double apply(double value, String operator, double term) {
		return switch (operator) {
			case "×" -> value * term;
			case "+" -> value + term;
			default -> value - term;
		};
	}

	static double amountOf(String term) {
		String bare = term.strip().replace("(", "").replace(")", "");
		boolean share = bare.endsWith("%");
		String number = bare.replace(" Gold", "").replace("%", "").strip().replace(".", "").replace(",", ".");
		double value = Double.parseDouble(number);
		return share ? value / 100 : value;
	}

	record Moved(String member, boolean deposit, int quantity, String item, int quality, boolean boughtAtTheMarketStall, boolean atTheGuildPrice, double share, int terms,
			double lastTerm, double result) {
		EvergoreItem catalogItem() {
			return CatalogLookup.itemFor(item).orElseThrow(() -> new AssertionError("not in the catalog: " + item));
		}

		boolean creditedAt(double wanted) {
			return deposit && !atTheGuildPrice && Math.abs(share - wanted) < EXACT;
		}

		boolean isTheSameMovementAs(Moved other) {
			return member.equals(other.member) && deposit == other.deposit && quantity == other.quantity && item.equals(other.item) && quality == other.quality;
		}

		boolean belowTheGuildPrice() {
			return deposit && !atTheGuildPrice && share < GUILD_PRICE - EXACT;
		}

		boolean aboveTheGuildPrice() {
			return deposit && !atTheGuildPrice && share > GUILD_PRICE + EXACT;
		}
	}
}
