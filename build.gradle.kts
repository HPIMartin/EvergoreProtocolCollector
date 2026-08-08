plugins {
	id("io.micronaut.application") version "4.6.2"
	id("com.diffplug.spotless") version "7.0.4"
	id("org.cyclonedx.bom") version "3.2.4"
	checkstyle
	jacoco
}

group = "dev.schoenberg.evergore"
version = "0.0.1-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	annotationProcessor("io.micronaut:micronaut-http-validation")
	annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
	annotationProcessor("io.micronaut.openapi:micronaut-openapi")

	implementation("io.micronaut:micronaut-inject")
	implementation("io.micronaut:micronaut-management")
	implementation("io.micronaut:micronaut-runtime")
	implementation("io.micronaut.validation:micronaut-validation")
	implementation("io.micronaut:micronaut-http-client")
	implementation("io.micronaut:micronaut-http-server-netty")
	implementation("io.micronaut:micronaut-jackson-databind")
	implementation("jakarta.annotation:jakarta.annotation-api")
	implementation("org.seleniumhq.selenium:selenium-java:4.7.2")
	implementation("com.j256.ormlite:ormlite-jdbc:6.1")
	implementation("org.xerial:sqlite-jdbc:3.41.2.2")

	runtimeOnly("ch.qos.logback:logback-classic")
	runtimeOnly("org.yaml:snakeyaml")

	testImplementation("io.micronaut.test:micronaut-test-junit5")
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testImplementation("org.junit.jupiter:junit-jupiter-params")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("com.konghq:unirest-java:3.11.11")
	testImplementation("org.assertj:assertj-core:3.27.7")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}

val frontendDistUsage: Attribute<String> = Attribute.of("dev.schoenberg.evergore.frontend-dist", String::class.java)

val frontendDistDependencies: Configuration by configurations.dependencyScope("frontendDistDependencies")

val frontendDist: Configuration by configurations.resolvable("frontendDist") {
	extendsFrom(frontendDistDependencies)
	attributes {
		attribute(frontendDistUsage, "spa")
	}
}

dependencies {
	frontendDistDependencies(project(":frontend"))
}

tasks.processResources {
	into("static/ui") {
		from(frontendDist)
	}
}

application {
	mainClass.set("dev.schoenberg.evergore.protocolParser.Application")
	applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
}

micronaut {
	version("4.10.3")
	runtime("netty")
	processing {
		incremental(true)
		annotations("dev.schoenberg.evergore.*")
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-serial", "-Werror"))
}

tasks.named<JavaCompile>("compileJava") {
	options.isFork = true
	options.forkOptions.jvmArgs = listOf(
		"-Dmicronaut.openapi.views.spec=rapidoc.enabled=true,swagger-ui.enabled=true,swagger-ui.theme=flattop"
	)
}

tasks.withType<Test> {
	useJUnitPlatform()
	jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// The active rules are not type-aware, so Checkstyle needs no compiled classpath. Emptying it drops
// `checkstyleMain -> classes -> processResources -> :frontend:npmBuild` from the pre-commit hook.
tasks.withType<Checkstyle>().configureEach {
	classpath = files()
}

spotless {
	java {
		target("src/**/*.java")
		targetExclude("src/probe/**")
		removeUnusedImports()
		importOrder("java", "javax", "jakarta", "", "dev.schoenberg", "\\#")
		eclipse().configFile("config/eclipse/formatter.xml")
		trimTrailingWhitespace()
		endWithNewline()
	}
	// The Gradle scripts get the whitespace basics only: no opinionated Kotlin formatter, because
	// every one of them indents with spaces while this codebase is tab-indented throughout.
	kotlinGradle {
		target("*.gradle.kts", "frontend/*.gradle.kts")
		leadingSpacesToTabs()
		trimTrailingWhitespace()
		endWithNewline()
	}
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)

	// Guard against re-adding `forkEvery`. Gradle restarts the test JVM per compiled *class file* of
	// the test source set (198 here, only 28 of which hold tests), and `--tests` does not reduce that
	// count because it filters inside the worker - so per-class forking cost ~8 min per suite run and
	// made focused runs unusable. It has twice been added as a crutch for a startup race that was
	// already fixed elsewhere and twice been proven unnecessary (2026-06-20, 2026-08-01). This check
	// is configuration-time on purpose: `forkEvery` is not a tracked task input, so a re-add would
	// leave `test` UP-TO-DATE and an execution-time check unreached.
	check(forkEvery == 0L) {
		"tasks.test.forkEvery is set to $forkEvery, but the suite is green in a single JVM. " +
				"Per-class forking costs ~8 min per run and breaks focused --tests runs. " +
				"See the test-execution model in docs/knowledge-base/testing.md before changing this."
	}
}

val probe: SourceSet by sourceSets.creating {
	compileClasspath += sourceSets["test"].output + sourceSets["test"].compileClasspath
	runtimeClasspath += sourceSets["test"].output + sourceSets["test"].runtimeClasspath
}

checkstyle {
	toolVersion = "10.21.0"
	configFile = file("config/checkstyle/checkstyle.xml")
	sourceSets = project.sourceSets.matching { it != probe }
}

configurations["probeAnnotationProcessor"].extendsFrom(configurations["testAnnotationProcessor"])

tasks.named<JavaCompile>("compileProbeJava") {
	options.compilerArgs.remove("-Werror")
}

tasks.register<Test>("probe") {
	group = "verification"
	description = "Runs the throwaway probes in src/probe/java; deliberately not wired into check or build."
	testClassesDirs = probe.output.classesDirs
	classpath = probe.runtimeClasspath
	failOnNoDiscoveredTests = true
}

tasks.register<Delete>("clearProbes") {
	group = "verification"
	description = "Deletes src/probe with everything in it, so removing a probe needs no rm."
	delete(layout.projectDirectory.dir("src/probe"))
}

val vulnScanFailOnSeverity = providers.gradleProperty("vulnScan.failOnSeverity").orNull?.takeIf { it.isNotBlank() }

tasks.register<Exec>("vulnScan") {
	group = "verification"
	description = "Scans the resolved dependencies for known vulnerabilities (CycloneDX SBOM analyzed by Trivy)."
	dependsOn(":cyclonedxDirectBom")
	dependsOn(":frontend:vulnScan")
	val bom = layout.buildDirectory.file("reports/cyclonedx-direct/bom.json")
	commandLine(buildList {
		add("trivy")
		add("sbom")
		vulnScanFailOnSeverity?.let {
			add("--severity")
			add(it)
			add("--exit-code")
			add("1")
		}
		add(bom.get().asFile.absolutePath)
	})
}

tasks.register<JavaExec>("generateAcceptanceDb") {
	group = "verification"
	description = "Generates src/test/resources/testdata.sqlite from the synthetic fixture dataset."
	classpath = sourceSets["test"].runtimeClasspath
	mainClass.set("dev.schoenberg.evergore.protocolParser.TestDataGenerator")
	jvmArgs("--enable-native-access=ALL-UNNAMED")
}
