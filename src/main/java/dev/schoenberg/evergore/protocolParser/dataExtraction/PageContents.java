package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.util.*;

public class PageContents {
	public final List<String> lager;
	public final List<String> bank;

	public PageContents(List<String> lager, List<String> bank) {
		this.lager = lager;
		this.bank = bank;
	}
}