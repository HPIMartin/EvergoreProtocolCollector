package {{BASE_PACKAGE}}.kbguard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnownJavaSymbolsTest {
    @Test
    void resolvesAClassFromThisProjectsOwnTestSource() {
        KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

        boolean resolved = tested.hasSimpleName("KbClassReferenceExtractor");

        assertThat(resolved).isTrue();
    }

    @Test
    void resolvesADependencyClassTheGuardNeverUses() {
        KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

        boolean resolved = tested.hasSimpleName("SoftAssertions");

        assertThat(resolved).isTrue();
    }

    @Test
    void resolvesAJdkClass() {
        KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

        boolean resolved = tested.hasSimpleName("ArrayList");

        assertThat(resolved).isTrue();
    }

    @Test
    void resolvesANestedClassByItsSimpleName() {
        KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

        boolean resolved = tested.hasSimpleName("SimpleEntry");

        assertThat(resolved).isTrue();
    }

    @Test
    void resolvesALocalClassByItsSimpleName() {
        class LocalProbeClass {
            String label() {
                return "local";
            }
        }
        String label = new LocalProbeClass().label();
        KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

        boolean resolved = tested.hasSimpleName("LocalProbeClass");

        assertThat(label).isEqualTo("local");
        assertThat(resolved).isTrue();
    }

    @Test
    void doesNotResolveAMadeUpClassName() {
        KnownJavaSymbols tested = KnownJavaSymbols.fromTestRuntimeClasspath();

        boolean resolved = tested.hasSimpleName("DatabaseStartupInitialization");

        assertThat(resolved).isFalse();
    }
}
