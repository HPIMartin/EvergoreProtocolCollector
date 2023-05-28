package dev.schoenberg.evergore.protocolParser.domain;

public record Item(int quantity, String name, int quality) {
	public String toString(String separator) {
		return quantity + separator + name + separator + quality;
	}
}
