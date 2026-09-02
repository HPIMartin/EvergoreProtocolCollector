package dev.schoenberg.evergore.protocolParser.arch;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

class KnownJavaSymbols {

	private final Set<String> simpleNames;

	private KnownJavaSymbols(Set<String> simpleNames) {
		this.simpleNames = simpleNames;
	}

	static KnownJavaSymbols fromTestRuntimeClasspath() {
		Set<String> simpleNames = new HashSet<>();
		for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
			collectSimpleNamesFrom(Path.of(entry), simpleNames);
		}
		collectSimpleNamesFromTheJdk(simpleNames);
		return new KnownJavaSymbols(simpleNames);
	}

	private static void collectSimpleNamesFromTheJdk(Set<String> simpleNames) {
		try (FileSystem jrt = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of())) {
			collectSimpleNamesFromDirectory(jrt.getPath("modules"), simpleNames);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void collectSimpleNamesFrom(Path classpathEntry, Set<String> simpleNames) {
		if (Files.isDirectory(classpathEntry)) {
			collectSimpleNamesFromDirectory(classpathEntry, simpleNames);
		} else if (Files.isRegularFile(classpathEntry) && classpathEntry.toString().endsWith(".jar")) {
			collectSimpleNamesFromJar(classpathEntry, simpleNames);
		}
	}

	private static void collectSimpleNamesFromDirectory(Path directory, Set<String> simpleNames) {
		try (Stream<Path> classFiles = Files.walk(directory)) {
			classFiles.filter(path -> path.toString().endsWith(".class")).map(path -> path.getFileName().toString()).forEach(fileName -> addSimpleName(fileName, simpleNames));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void collectSimpleNamesFromJar(Path jarFile, Set<String> simpleNames) {
		try (JarFile jar = new JarFile(jarFile.toFile())) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				String entryName = entries.nextElement().getName();
				if (entryName.endsWith(".class")) {
					int lastSlash = entryName.lastIndexOf('/');
					addSimpleName(entryName.substring(lastSlash + 1), simpleNames);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void addSimpleName(String classFileName, Set<String> simpleNames) {
		String withoutExtension = classFileName.substring(0, classFileName.length() - ".class".length());
		String afterLastDollar = withoutExtension.substring(withoutExtension.lastIndexOf('$') + 1);
		String simpleName = stripLeadingLocalClassOrdinal(afterLastDollar);
		if (!simpleName.isEmpty() && !simpleName.chars().allMatch(Character::isDigit)) {
			simpleNames.add(simpleName);
		}
	}

	private static String stripLeadingLocalClassOrdinal(String candidate) {
		int index = 0;
		while (index < candidate.length() && Character.isDigit(candidate.charAt(index))) {
			index++;
		}
		return candidate.substring(index);
	}

	boolean hasSimpleName(String simpleName) {
		return simpleNames.contains(simpleName);
	}
}
