package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.net.URI;

public class TokenOpening {
	private static final String TOKEN = "test-token";

	private final MemberBrowser browser;
	private String requestedDeepLink;

	TokenOpening(MemberBrowser browser) {
		this.browser = browser;
	}

	public void open(String path) {
		browser.openHref(path + tokenParameterSeparator(path) + "token=" + TOKEN);
	}

	public void openTheStartPage(TokenChoice tokenChoice) {
		browser.openHref(tokenChoice == TokenChoice.WRONG_TOKEN ? "/?token=wrong-token" : "/");
	}

	public void openADeepLinkBookmark(String path, TokenChoice tokenChoice) {
		String href = tokenChoice == TokenChoice.WRONG_TOKEN ? path + tokenParameterSeparator(path) + "token=wrong-token" : path;
		requestedDeepLink = path;
		browser.driver().get(browser.addressOf(href));
	}

	public boolean isAtTheRequestedDeepLink() {
		awaitTheDocumentLoaded();
		return requestedDeepLink != null && URI.create(browser.driver().getCurrentUrl()).getRawPath().equals(URI.create(requestedDeepLink).getRawPath());
	}

	public boolean showsNoPageBrand() {
		awaitTheDocumentLoaded();
		return Boolean.TRUE.equals(browser.execute("return document.querySelector('[data-testid=page-brand]') === null"));
	}

	private void awaitTheDocumentLoaded() {
		browser.awaitUntil("document.readyState === 'complete'");
	}

	private static String tokenParameterSeparator(String path) {
		return path.indexOf('?') < 0 ? "?" : "&";
	}
}
