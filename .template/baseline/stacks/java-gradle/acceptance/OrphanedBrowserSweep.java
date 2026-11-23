package {{BASE_PACKAGE}}.acceptance;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import {{BASE_PACKAGE}}.acceptance.browser.OwnBuildDirectory;

import static java.lang.String.join;
import static java.time.Instant.now;

public final class OrphanedBrowserSweep {
    enum Selector { ANCESTRY, MARKER }

    private static final Selector SELECTOR = Selector.MARKER;
    private static final String DRIVER = "geckodriver";
    private static final String BROWSER = "firefox";
    private static final String JVM = "java";

    private OrphanedBrowserSweep() {}

    record Swept(long pid, String description) {}

    public static void sweep() {
        failOnAnyLeftover(sweepOrphanedBrowsers(SELECTOR, ProcessHandle.allProcesses()));
    }

    static boolean namesDirectory(String commandLine, Path directory) {
        return Pattern.compile("(?<![A-Za-z0-9._/-])" + Pattern.quote(directory.toString()) + "(?![A-Za-z0-9._-])").matcher(commandLine).find();
    }

    static List<Swept> sweepOrphanedBrowsers(Selector selector, Stream<ProcessHandle> candidates) {
        Path build = OwnBuildDirectory.path();
        List<ProcessHandle> orphans = candidates
                .filter(OrphanedBrowserSweep::isABrowserProcess)
                .filter(OrphanedBrowserSweep::belongsToNoRunningTest)
                .filter(process -> selector == Selector.ANCESTRY || namesDirectory(commandLineOf(process), build))
                .toList();
        List<Swept> swept = orphans.stream().map(process -> new Swept(process.pid(), describe(process))).toList();

        orphans.forEach(ProcessHandle::destroyForcibly);

        return swept;
    }

    static void failOnAnyLeftover(List<Swept> swept) {
        if (swept.isEmpty()) {
            return;
        }
        throw new IllegalStateException("killed browser processes that outlived the run which started them: "
                + join(", ", swept.stream().map(Swept::description).toList())
                + ". That run reported nothing, so this sweep fails in its place.");
    }

    private static boolean isABrowserProcess(ProcessHandle process) {
        return executableOf(process).map(name -> name.equals(DRIVER) || name.startsWith(BROWSER)).orElse(false);
    }

    private static boolean belongsToNoRunningTest(ProcessHandle process) {
        for (Optional<ProcessHandle> ancestor = process.parent(); ancestor.isPresent(); ancestor = ancestor.get().parent()) {
            if (isALivingJvm(ancestor.get())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isALivingJvm(ProcessHandle ancestor) {
        return ancestor.isAlive() && executableOf(ancestor).filter(JVM::equals).isPresent();
    }

    private static String commandLineOf(ProcessHandle process) {
        return process.info().commandLine().orElse("");
    }

    private static Optional<String> executableOf(ProcessHandle process) {
        return process.info().command().map(command -> command.substring(command.lastIndexOf('/') + 1));
    }

    private static String describe(ProcessHandle process) {
        return executableOf(process).orElse("an unnamed browser process") + " [pid " + process.pid() + ", " + ageOf(process) + " old]";
    }

    private static String ageOf(ProcessHandle process) {
        Instant startedAt = process.info().startInstant().orElse(now());
        Duration age = Duration.between(startedAt, now());
        return age.toHours() + " h " + age.toMinutesPart() + " min";
    }
}
