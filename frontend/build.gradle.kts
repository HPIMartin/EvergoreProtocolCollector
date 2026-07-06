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

tasks.assemble {
	dependsOn(npmBuild)
}

tasks.check {
	dependsOn(npmTest)
}
