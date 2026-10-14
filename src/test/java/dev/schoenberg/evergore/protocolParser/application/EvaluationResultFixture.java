package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTrip;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTripAbstention;

public class EvaluationResultFixture {
	private List<String> unknownItemNames = List.of();
	private List<String> zeroValuedItemNames = List.of();
	private List<String> failedAvatarNames = List.of();
	private List<RoundTrip> roundTrips = List.of();
	private List<RoundTripAbstention> roundTripAbstentions = List.of();

	public EvaluationResultFixture unknownItemNames(List<String> unknownItemNames) {
		this.unknownItemNames = unknownItemNames;
		return this;
	}

	public EvaluationResultFixture zeroValuedItemNames(List<String> zeroValuedItemNames) {
		this.zeroValuedItemNames = zeroValuedItemNames;
		return this;
	}

	public EvaluationResultFixture failedAvatarNames(List<String> failedAvatarNames) {
		this.failedAvatarNames = failedAvatarNames;
		return this;
	}

	public EvaluationResultFixture roundTrips(List<RoundTrip> roundTrips) {
		this.roundTrips = roundTrips;
		return this;
	}

	public EvaluationResultFixture roundTripAbstentions(List<RoundTripAbstention> roundTripAbstentions) {
		this.roundTripAbstentions = roundTripAbstentions;
		return this;
	}

	public EvaluationResult build() {
		return new EvaluationResult(unknownItemNames, zeroValuedItemNames, failedAvatarNames, roundTrips, roundTripAbstentions);
	}
}
