package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

final class RecordingWebDriver implements WebDriver {
	private final Map<By, String> clickTargets = new HashMap<>();
	private final Map<By, List<String>> sentKeys = new HashMap<>();
	private RuntimeException navigateFailure;
	private RuntimeException quitFailure;
	private String currentUrl = "";
	private boolean quitCalled;

	void navigateOnClick(By locator, String target) {
		clickTargets.put(locator, target);
	}

	void stopNavigating() {
		clickTargets.clear();
	}

	List<String> keysSentTo(By locator) {
		return sentKeys.getOrDefault(locator, List.of());
	}

	void failOnNavigate(RuntimeException failure) {
		navigateFailure = failure;
	}

	void failOnQuit(RuntimeException failure) {
		quitFailure = failure;
	}

	boolean quitCalled() {
		return quitCalled;
	}

	@Override
	public void quit() {
		quitCalled = true;
		if (quitFailure != null) {
			throw quitFailure;
		}
	}

	@Override
	public String getCurrentUrl() {
		return currentUrl;
	}

	@Override
	public WebElement findElement(By by) {
		return new StubWebElement(by, this);
	}

	private void clicked(By locator) {
		String target = clickTargets.get(locator);
		if (target != null) {
			currentUrl = target;
		}
	}

	private void keysSent(By locator, CharSequence... keys) {
		sentKeys.computeIfAbsent(locator, ignored -> new ArrayList<>()).add(String.join("", keys));
	}

	@Override
	public Navigation navigate() {
		return new Navigation() {
			@Override
			public void to(String url) {
				if (navigateFailure != null) {
					throw navigateFailure;
				}
				currentUrl = url;
			}

			@Override
			public void to(URL url) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void back() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void forward() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void refresh() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public void close() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void get(String url) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTitle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<WebElement> findElements(By by) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPageSource() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getWindowHandles() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getWindowHandle() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TargetLocator switchTo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Options manage() {
		throw new UnsupportedOperationException();
	}

	private static final class StubWebElement implements WebElement {
		private final By locator;
		private final RecordingWebDriver driver;

		private StubWebElement(By locator, RecordingWebDriver driver) {
			this.locator = locator;
			this.driver = driver;
		}

		@Override
		public void click() {
			driver.clicked(locator);
		}

		@Override
		public String getText() {
			return "";
		}

		@Override
		public void submit() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void sendKeys(CharSequence... keysToSend) {
			driver.keysSent(locator, keysToSend);
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getTagName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getAttribute(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isSelected() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEnabled() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<WebElement> findElements(By by) {
			throw new UnsupportedOperationException();
		}

		@Override
		public WebElement findElement(By by) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isDisplayed() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Point getLocation() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Dimension getSize() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Rectangle getRect() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getCssValue(String propertyName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <X> X getScreenshotAs(OutputType<X> target) {
			throw new UnsupportedOperationException();
		}
	}
}
