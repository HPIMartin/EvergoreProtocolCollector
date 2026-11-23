package {{BASE_PACKAGE}}.kbguard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class KnownFrontendSymbolsTest {
    @TempDir
    Path frontend;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "export class Widget extends Base {}|Widget",
            "export abstract class Panel {}|Panel",
            "export interface LoginAnswer { ok: boolean }|LoginAnswer",
            "export type SortKey = string|SortKey",
            "export enum Phase { Open }|Phase",
            "const enum Direction { Up }|Direction",
            "declare namespace Telemetry {}|Telemetry",
            "export function Gadget() {}|Gadget",
            "export async function Loader() {}|Loader",
            "export function* Counter() {}|Counter",
            "export function *Ticker() {}|Ticker",
            "export const StatusPanel = () => null|StatusPanel",
            "let Registry = new Map()|Registry",
            "var LegacyHolder = {}|LegacyHolder"})
    void resolvesEachFormOfDeclaration(String source, String declared) throws IOException {
        Files.writeString(frontend.resolve("source.ts"), source + "\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName(declared);

        assertThat(resolved).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ts", "tsx", "js", "jsx", "mjs", "cjs", "mts", "cts", "vue", "svelte"})
    void readsTheDeclarationsOfEachSourceFileExtension(String extension) throws IOException {
        Files.writeString(frontend.resolve("holder." + extension), "export class ExtensionHeld {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName("ExtensionHeld");

        assertThat(resolved).isTrue();
    }

    @Test
    void ignoresADeclarationInAFileOfAnotherExtension() throws IOException {
        Files.writeString(frontend.resolve("notes.md"), "export class NotesOnly {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName("NotesOnly");

        assertThat(resolved).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ts", "vue", "js"})
    void readsNoDeclarationsOfAFileWhoseWholeNameIsASourceExtension(String fileName) throws IOException {
        Files.writeString(frontend.resolve(fileName), "export class DotlessHeld {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName("DotlessHeld");

        assertThat(resolved).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"LoginPage.vue,LoginPage", "App.tsx,App", "useThing.test.ts,useThing"})
    void resolvesTheBaseNameOfASourceFile(String fileName, String baseName) throws IOException {
        Files.writeString(frontend.resolve(fileName), "export default {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName(baseName);

        assertThat(resolved).isTrue();
    }

    @Test
    void walksIntoSubdirectories() throws IOException {
        Files.createDirectories(frontend.resolve("ui/widgets"));
        Files.writeString(frontend.resolve("ui/widgets/deep.ts"), "export class DeepWidget {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName("DeepWidget");

        assertThat(resolved).isTrue();
    }

    @Test
    void doesNotResolveANameDeclaredOnlyUnderNodeModules() throws IOException {
        Files.createDirectories(frontend.resolve("node_modules/library"));
        Files.createDirectories(frontend.resolve("app/node_modules"));
        Files.writeString(frontend.resolve("node_modules/library/index.ts"), "export class VendoredTop {}\n");
        Files.writeString(frontend.resolve("app/node_modules/inner.ts"), "export class VendoredNested {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean topResolved = tested.hasName("VendoredTop");
        boolean nestedResolved = tested.hasName("VendoredNested");

        assertThat(topResolved).isFalse();
        assertThat(nestedResolved).isFalse();
    }

    @Test
    void doesNotResolveAnUndeclaredName() throws IOException {
        Files.writeString(frontend.resolve("source.ts"), "export class Declared {}\nconst usage = new Undeclared();\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName("Undeclared");

        assertThat(resolved).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Array", "ArrayBuffer", "Atomics", "BigInt", "Date", "Intl", "JSON", "Map", "Math", "Promise", "Reflect", "Symbol", "WeakMap", "Temporal", "Float16Array"})
    void resolvesAPlatformNameWithARoot(String platformName) {
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName(platformName);

        assertThat(resolved).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Date", "Intl", "Temporal", "Float16Array"})
    void resolvesAPlatformNameWithoutARoot(String platformName) {
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, "");

        boolean resolved = tested.hasName(platformName);

        assertThat(resolved).isTrue();
    }

    @Test
    void readsNoSourceForAnEmptyRootEvenWhenTheProjectDirectoryHoldsSources() throws IOException {
        Files.writeString(frontend.resolve("top-level.ts"), "export class TopLevelProbeClass {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, "");

        boolean resolved = tested.hasName("TopLevelProbeClass");

        assertThat(resolved).isFalse();
    }

    @Test
    void resolvesASetRootAgainstTheProjectDirectory() throws IOException {
        Files.createDirectories(frontend.resolve("web/src"));
        Files.writeString(frontend.resolve("web/src/page.ts"), "export class UnderTheRoot {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, "web/src");

        boolean resolved = tested.hasName("UnderTheRoot");

        assertThat(resolved).isTrue();
    }

    @Test
    void declaresNothingForAKeywordThatEndsAnotherWord() throws IOException {
        Files.writeString(frontend.resolve("source.ts"), "metaclass Hidden {}\n");
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, ".");

        boolean resolved = tested.hasName("Hidden");

        assertThat(resolved).isFalse();
    }

    @Test
    void failsNamingARootThatIsARegularFile() throws IOException {
        Files.writeString(frontend.resolve("source.ts"), "export class Held {}\n");

        Throwable failure = catchThrowable(() -> KnownFrontendSymbols.fromSourceRoot(frontend, "source.ts"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("source.ts");
    }

    @Test
    void readsARootThatIsASymbolicLink() throws IOException {
        Path real = Files.createDirectories(frontend.resolve("real-src"));
        Files.writeString(real.resolve("page.ts"), "export class ThroughTheLink {}\n");
        linkOrSkip(frontend.resolve("linked-src"), real);
        KnownFrontendSymbols tested = KnownFrontendSymbols.fromSourceRoot(frontend, "linked-src");

        boolean resolved = tested.hasName("ThroughTheLink");

        assertThat(resolved).isTrue();
    }

    @Test
    void skipsADanglingSymbolicLinkWithASourceExtension() {
        linkOrSkip(frontend.resolve(".#lock.ts"), frontend.resolve("missing-target"));

        Throwable failure = catchThrowable(() -> KnownFrontendSymbols.fromSourceRoot(frontend, "."));

        assertThat(failure).isNull();
    }

    private static void linkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symbolic links are not available here");
        }
    }

    @Test
    void failsNamingARootThatIsSetAndIsNoDirectory() {
        Throwable failure = catchThrowable(() -> KnownFrontendSymbols.fromSourceRoot(frontend, "not-there"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("not-there");
    }
}
