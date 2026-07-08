import com.github.gradle.node.npm.task.NpmTask

plugins {
	base
	id("com.github.node-gradle.node") version "7.1.0"
}

node {
	version = providers.gradleProperty("nodeVersion").get()
	download = true
	npmInstallCommand = "ci"
	fastNpmInstall = true
}

val frontendDistUsage: Attribute<String> = Attribute.of("dev.schoenberg.evergore.frontend-dist", String::class.java)

configurations {
	consumable("frontendDist") {
		attributes {
			attribute(frontendDistUsage, "spa")
		}
	}
}

val npmBuild = tasks.register<NpmTask>("npmBuild") {
	group = "build"
	description = "Builds the SPA with vite into build/dist."
	dependsOn(tasks.npmInstall)
	npmCommand.set(listOf("run", "build"))
	inputs.files("package.json", "package-lock.json", "vite.config.ts", "index.html").withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.files(fileTree(".") { include("tsconfig*.json") }).withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.dir("src").withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.files(fileTree("public")).withPathSensitivity(PathSensitivity.RELATIVE)
	outputs.dir(layout.buildDirectory.dir("dist"))
	outputs.cacheIf { true }
}

artifacts {
	add("frontendDist", layout.buildDirectory.dir("dist")) {
		builtBy(npmBuild)
	}
}

val npmTest = tasks.register<NpmTask>("npmTest") {
	group = "verification"
	description = "Runs the frontend component tests with vitest."
	dependsOn(tasks.npmInstall)
	npmCommand.set(listOf("run", "test"))
	inputs.files("package.json", "package-lock.json", "vite.config.ts").withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.files(fileTree(".") { include("tsconfig*.json") }).withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.dir("src").withPathSensitivity(PathSensitivity.RELATIVE)
	outputs.file(layout.buildDirectory.file("reports/vitest/results.xml"))
	outputs.cacheIf { true }
}

val npmLint = tasks.register<NpmTask>("npmLint") {
	group = "verification"
	description = "Lints and format-checks the frontend with ESLint and Prettier."
	dependsOn(tasks.npmInstall)
	npmCommand.set(listOf("run", "lint"))
	inputs.files("package.json", "package-lock.json", "eslint.config.js", ".prettierrc.json", ".prettierignore")
		.withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.files(fileTree(".") { include("tsconfig*.json") }).withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.file("vite.config.ts").withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.file("index.html").withPathSensitivity(PathSensitivity.RELATIVE)
	inputs.dir("src").withPathSensitivity(PathSensitivity.RELATIVE)
	outputs.file(layout.buildDirectory.file("reports/eslint/results.txt"))
	outputs.cacheIf { true }
}

tasks.assemble {
	dependsOn(npmBuild)
}

tasks.check {
	dependsOn(npmTest)
	dependsOn(npmLint)
}

val vulnScanFailOnSeverity = providers.gradleProperty("vulnScan.failOnSeverity").orNull?.takeIf { it.isNotBlank() }

tasks.register<Exec>("vulnScan") {
	group = "verification"
	description = "Scans the npm dependencies for known vulnerabilities with Trivy."
	commandLine(buildList {
		add("trivy")
		add("fs")
		add("--scanners")
		add("vuln")
		add("--skip-dirs")
		add("node_modules")
		vulnScanFailOnSeverity?.let {
			add("--severity")
			add(it)
			add("--exit-code")
			add("1")
		}
		add(".")
	})
}
