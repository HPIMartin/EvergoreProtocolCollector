package {{BASE_PACKAGE}}.acceptance.browser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class OwnBuildDirectory {
    private static final String BROWSER_PROFILES = "tmp/browsers";
    private static final String DOWNLOADS = "tmp/downloads";

    private OwnBuildDirectory() {}

    public static Path path() {
        return realPathOf(Path.of("build"));
    }

    static Path realPathOf(Path directory) {
        Path absolute = directory.toAbsolutePath().normalize();
        try {
            return Files.exists(absolute) ? absolute.toRealPath() : absolute;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Path downloads() {
        return path().resolve(DOWNLOADS);
    }

    static Path browserProfiles() {
        return path().resolve(BROWSER_PROFILES);
    }
}
