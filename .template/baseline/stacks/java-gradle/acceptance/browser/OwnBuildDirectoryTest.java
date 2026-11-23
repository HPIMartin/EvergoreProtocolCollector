package {{BASE_PACKAGE}}.acceptance.browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class OwnBuildDirectoryTest {
    @TempDir
    Path checkout;

    @Test
    void aBuildDirectoryReachedThroughASymbolicLinkIsNamedByItsRealPath() throws IOException {
        Path real = Files.createDirectories(checkout.resolve("elsewhere/build"));
        Path link = Files.createSymbolicLink(checkout.resolve("build"), real);

        Path resolved = OwnBuildDirectory.realPathOf(link);

        assertThat(resolved).isEqualTo(real.toRealPath());
    }

    @Test
    void aBuildDirectoryThatDoesNotExistYetIsNamedAsGiven() {
        Path notYet = checkout.resolve("a/../build");

        Path resolved = OwnBuildDirectory.realPathOf(notYet);

        assertThat(resolved).isEqualTo(checkout.resolve("build").toAbsolutePath().normalize());
    }
}
