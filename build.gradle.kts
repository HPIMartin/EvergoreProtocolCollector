plugins {
	id("io.micronaut.application") version "4.6.2"
	id("com.diffplug.spotless") version "7.0.4"
	checkstyle
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
	implementation("io.micronaut:micronaut-runtime")
	implementation("io.micronaut.validation:micronaut-validation")
	implementation("io.micronaut:micronaut-http-client")
	implementation("io.micronaut:micronaut-http-server-netty")
	implementation("io.micronaut:micronaut-jackson-databind")
	implementation("jakarta.annotation:jakarta.annotation-api")
	implementation("org.seleniumhq.selenium:selenium-java:4.7.2")
	implementation("com.j256.ormlite:ormlite-jdbc:6.1")
	implementation("org.xerial:sqlite-jdbc:3.41.2.2")
	implementation("org.apache.commons:commons-text:1.10.0")

	runtimeOnly("ch.qos.logback:logback-classic")
	runtimeOnly("org.yaml:snakeyaml")

	testImplementation("io.micronaut.test:micronaut-test-junit5")
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("com.konghq:unirest-java:3.11.11")
	testImplementation("org.assertj:assertj-core:3.27.7")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
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

checkstyle {
	toolVersion = "10.21.0"
	configFile = file("config/checkstyle/checkstyle.xml")
}

spotless {
	java {
		target("src/**/*.java")
		removeUnusedImports()
		importOrder("java", "javax", "jakarta", "", "dev.schoenberg", "\\#")
		eclipse().configFile("config/eclipse/formatter.xml")
		trimTrailingWhitespace()
		endWithNewline()
	}
}
