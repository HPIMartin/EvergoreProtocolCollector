package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record Navigation(List<FrameLink> frame, List<String> allHrefs) {
	public record FrameLink(String label, String href, boolean current) {}
}
