package {{BASE_PACKAGE}}.acceptance.browser;

import java.lang.System.Logger;
import java.util.concurrent.atomic.AtomicInteger;

import org.opentest4j.TestAbortedException;

import static java.lang.System.Logger.Level.INFO;

public final class SkippedScenarios {
    private static final Logger LOG = System.getLogger(SkippedScenarios.class.getName());
    private static final SkippedScenarios THIS_RUN = new SkippedScenarios();

    private final AtomicInteger count = new AtomicInteger();

    SkippedScenarios() {}

    static SkippedScenarios ofThisRun() {
        return THIS_RUN;
    }

    TestAbortedException skip(String reason) {
        count.incrementAndGet();
        return new TestAbortedException(reason);
    }

    int count() {
        return count.get();
    }

    public static void report() {
        int skipped = THIS_RUN.count();
        if (skipped > 0) {
            LOG.log(INFO, "{0} browser-driven scenario(s) skipped: no browser here and ACCEPTANCE_NO_BROWSER is set", skipped);
        }
    }
}
