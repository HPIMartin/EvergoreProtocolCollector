package {{BASE_PACKAGE}}.acceptance.browser;

import java.lang.reflect.Proxy;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;

final class RecordingDriver {
    private int quits;
    private Throwable quitFailure;
    private Object scriptAnswer;
    private final WebDriver proxy = (WebDriver) Proxy.newProxyInstance(WebDriver.class.getClassLoader(), new Class<?>[] {WebDriver.class, JavascriptExecutor.class},
            (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "equals":
                        return proxy == arguments[0];
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "toString":
                        return "recording driver";
                    case "executeScript":
                        return scriptAnswer;
                    case "quit":
                        quits++;
                        if (quitFailure != null) {
                            if (quitFailure instanceof Error error) {
                                throw error;
                            }
                            throw (RuntimeException) quitFailure;
                        }
                        return null;
                    default:
                        return null;
                }
            });

    WebDriver proxy() {
        return proxy;
    }

    int quits() {
        return quits;
    }

    void failQuitWith(String message) {
        quitFailure = new IllegalStateException(message);
    }

    void failQuitWithError(String message) {
        quitFailure = new AssertionError(message);
    }

    void failQuitAsTheSessionHasEnded() {
        quitFailure = new NoSuchSessionException("session ended");
    }

    void answerScriptsWith(Object answer) {
        scriptAnswer = answer;
    }
}
