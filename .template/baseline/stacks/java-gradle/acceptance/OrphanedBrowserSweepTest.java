package {{BASE_PACKAGE}}.acceptance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import {{BASE_PACKAGE}}.acceptance.OrphanedBrowserSweep.Selector;
import {{BASE_PACKAGE}}.acceptance.OrphanedBrowserSweep.Swept;
import {{BASE_PACKAGE}}.acceptance.browser.OwnBuildDirectory;

import static {{BASE_PACKAGE}}.acceptance.OrphanedBrowserSweep.failOnAnyLeftover;
import static {{BASE_PACKAGE}}.acceptance.OrphanedBrowserSweep.namesDirectory;
import static {{BASE_PACKAGE}}.acceptance.OrphanedBrowserSweep.sweepOrphanedBrowsers;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrphanedBrowserSweepTest {
    private static final Path MARKED_PLANTS = OwnBuildDirectory.path().resolve("tmp/orphanedBrowserSweep");
    private static final Path OUTSIDE_THE_BUILD_DIRECTORY = Path.of("/tmp");
    private static final Path A_BINARY_THAT_IGNORES_ITS_OWN_NAME = Path.of("/usr/bin/perl");
    private static final String LIVE_AS_LONG_AS_THE_OWNER_LIVES = "$| = 1; print \"$$\\n\"; my $owner = shift; sleep 1 while kill 0, $owner;";
    private static final String LEAVE_A_CHILD_THAT_LIVES_AS_LONG_AS_THE_OWNER_LIVES = "$| = 1; my $owner = shift; my $child = fork; if ($child) { print \"$child\\n\"; <STDIN>; exit } sleep 1 while kill 0, $owner;";
    private static final String DRIVER = "geckodriver";
    private static final String BROWSER = "firefox-esr";
    private static final String FOREIGN_BROWSER = "firefox";
    private static final String NO_BROWSER = "helper";

    private static ProcessHandle bystander;
    private static Path bystanderRoot;

    private final List<ProcessHandle> planted = new ArrayList<>();
    private final List<Path> unmarkedRoots = new ArrayList<>();

    @BeforeAll
    static void plantAForeignBrowserThatNoCaseMayTouch() throws Exception {
        bystanderRoot = Files.createTempDirectory(OUTSIDE_THE_BUILD_DIRECTORY, "sweepBystander");
        bystander = plantDetached(bystanderRoot, FOREIGN_BROWSER, new ArrayList<>());
    }

    @AfterAll
    static void killTheForeignBrowser() throws IOException {
        bystander.destroyForcibly();
        deleteTree(bystanderRoot);
    }

    @AfterEach
    void killWhateverSurvivedTheCase() throws IOException {
        planted.forEach(ProcessHandle::destroyForcibly);
        for (Path root : unmarkedRoots) {
            deleteTree(root);
        }
        assertThat(bystander.isAlive()).as("the foreign browser no case may touch").isTrue();
    }

    @Test
    void theAncestrySelectorKillsAMarkedBrowserProcessWhoseTestRunIsGone() throws Exception {
        ProcessHandle orphan = plantDetached(markedRoot("ancestryMarked"), DRIVER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.ANCESTRY, Stream.of(orphan));

        assertThat(swept).extracting(Swept::pid).containsExactly(orphan.pid());
        assertIsGone(orphan);
    }

    @Test
    void theAncestrySelectorKillsAnUnmarkedBrowserProcessWhoseTestRunIsGone() throws Exception {
        ProcessHandle orphan = plantDetached(unmarkedRoot(), DRIVER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.ANCESTRY, Stream.of(orphan));

        assertThat(swept).extracting(Swept::pid).containsExactly(orphan.pid());
        assertIsGone(orphan);
    }

    @Test
    void theMarkerSelectorKillsADriverProcessNamingThisBuildDirectoryWhoseTestRunIsGone() throws Exception {
        ProcessHandle orphan = plantDetached(markedRoot("markerMarkedDriver"), DRIVER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.MARKER, Stream.of(orphan));

        assertThat(swept).extracting(Swept::pid).containsExactly(orphan.pid());
        assertIsGone(orphan);
    }

    @Test
    void theMarkerSelectorKillsAFirefoxEsrProcessNamingThisBuildDirectoryWhoseTestRunIsGone() throws Exception {
        ProcessHandle orphan = plantDetached(markedRoot("markerMarkedBrowser"), BROWSER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.MARKER, Stream.of(orphan));

        assertThat(swept).extracting(Swept::pid).containsExactly(orphan.pid());
        assertIsGone(orphan);
    }

    @Test
    void theMarkerSelectorSparesADriverThatNamesNoBuildDirectoryOfThisCheckout() throws Exception {
        ProcessHandle stranger = plantDetached(unmarkedRoot(), DRIVER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.MARKER, Stream.of(stranger));

        assertThat(swept).isEmpty();
        assertThat(stranger.isAlive()).isTrue();
    }

    @Test
    void theMarkerSelectorSparesAFirefoxEsrThatNamesNoBuildDirectoryOfThisCheckout() throws Exception {
        ProcessHandle stranger = plantDetached(unmarkedRoot(), BROWSER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.MARKER, Stream.of(stranger));

        assertThat(swept).isEmpty();
        assertThat(stranger.isAlive()).isTrue();
    }

    @Test
    void aMarkedProcessThatIsNoBrowserIsSpared() throws Exception {
        ProcessHandle helper = plantDetached(markedRoot("noBrowser"), NO_BROWSER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.ANCESTRY, Stream.of(helper));

        assertThat(swept).isEmpty();
        assertThat(helper.isAlive()).isTrue();
    }

    @Test
    void anOrphanOutsideTheGivenCandidatesIsSpared() throws Exception {
        ProcessHandle orphan = plantDetached(markedRoot("outsideTheCandidates"), DRIVER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.ANCESTRY, Stream.empty());

        assertThat(swept).isEmpty();
        assertThat(orphan.isAlive()).isTrue();
    }

    @Test
    void theAncestrySelectorLeavesTheBrowserOfARunningTestRunAlone() throws Exception {
        ProcessHandle inUse = plantUnderThisJvm(markedRoot("ancestryInUse"), DRIVER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.ANCESTRY, Stream.of(inUse));

        assertThat(swept).isEmpty();
        assertThat(inUse.isAlive()).isTrue();
    }

    @Test
    void theMarkerSelectorLeavesTheBrowserOfARunningTestRunAlone() throws Exception {
        ProcessHandle inUse = plantUnderThisJvm(markedRoot("markerInUse"), DRIVER);

        List<Swept> swept = sweepOrphanedBrowsers(Selector.MARKER, Stream.of(inUse));

        assertThat(swept).isEmpty();
        assertThat(inUse.isAlive()).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/w/geckodriver --profile-root /w/build/tmp/browsers|true",
            "/w/geckodriver --profile-root=/w/build/tmp/browsers|true",
            "/w/build/tmp/geckodriver -e x|true",
            "/w/geckodriver --profile-root /w/build|true",
            "/w/geckodriver --profile-root /w/build-old/tmp|false",
            "/w/geckodriver --profile-root /w/build.old/tmp|false",
            "/w/geckodriver --profile-root /w/builds/tmp|false",
            "/w2/geckodriver --profile-root /w2/build/tmp|false",
            "/x/w/geckodriver --profile-root /x/w/build/tmp|false",
            "/usr/bin/firefox -profile /srv/profiles/p|false"})
    void theMarkerNamesTheBuildDirectoryOnlyAsAWholePathSegment(String commandLine, boolean expected) {
        boolean named = namesDirectory(commandLine, Path.of("/w/build"));

        assertThat(named).isEqualTo(expected);
    }

    @Test
    void failsTheRunNamingWhatTheSweepKilled() {
        Throwable outcome = catchThrowable(() -> failOnAnyLeftover(List.of(new Swept(4711, "geckodriver [pid 4711, 0 h 3 min old]"))));

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessageContaining("geckodriver [pid 4711, 0 h 3 min old]");
    }

    @Test
    void staysSilentWhenNothingWasLeftBehind() {
        Throwable outcome = catchThrowable(() -> failOnAnyLeftover(List.of()));

        assertThat(outcome).isNull();
    }

    private static Path markedRoot(String plantName) {
        return MARKED_PLANTS.resolve(plantName);
    }

    private Path unmarkedRoot() throws IOException {
        Path root = Files.createTempDirectory(OUTSIDE_THE_BUILD_DIRECTORY, "sweepUnmarked");
        unmarkedRoots.add(root);
        return root;
    }

    private ProcessHandle plantUnderThisJvm(Path root, String name) throws IOException {
        Process plant = new ProcessBuilder(fakeBinaryNamedIn(root, name).toString(), "-e", LIVE_AS_LONG_AS_THE_OWNER_LIVES, ownPid()).redirectErrorStream(true).start();
        planted.add(plant.toHandle());
        pidPrintedBy(plant);
        return plant.toHandle();
    }

    private ProcessHandle plantDetached(Path root, String name) throws Exception {
        return plantDetached(root, name, planted);
    }

    private static ProcessHandle plantDetached(Path root, String name, List<ProcessHandle> registry) throws Exception {
        Process launcher = new ProcessBuilder(fakeBinaryNamedIn(root, name).toString(), "-e", LEAVE_A_CHILD_THAT_LIVES_AS_LONG_AS_THE_OWNER_LIVES, ownPid()).redirectErrorStream(true).start();
        registry.add(launcher.toHandle());
        ProcessHandle plant = ProcessHandle.of(pidPrintedBy(launcher)).orElseThrow();
        registry.add(plant);
        launcher.getOutputStream().close();
        launcher.waitFor();
        return plant;
    }

    private static String ownPid() {
        return String.valueOf(ProcessHandle.current().pid());
    }

    private static long pidPrintedBy(Process plant) throws IOException {
        BufferedReader output = new BufferedReader(new InputStreamReader(plant.getInputStream()));
        String line = output.readLine();
        if (line == null) {
            throw new IllegalStateException("the planted process printed no pid before its output closed");
        }
        return Long.parseLong(line.trim());
    }

    private static Path fakeBinaryNamedIn(Path root, String name) throws IOException {
        Path binary = root.resolve(name);
        Files.createDirectories(root);
        Files.copy(A_BINARY_THAT_IGNORES_ITS_OWN_NAME, binary, REPLACE_EXISTING);
        binary.toFile().setExecutable(true);
        return binary;
    }

    private static void assertIsGone(ProcessHandle killed) {
        killed.onExit().join();

        assertThat(killed.isAlive()).isFalse();
    }

    private static void deleteTree(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            files.sorted(Comparator.reverseOrder()).forEach(OrphanedBrowserSweepTest::deleteQuietly);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
