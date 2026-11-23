package {{BASE_PACKAGE}}.kbguard;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class KnownFrontendSymbols {
    static final Set<String> PLATFORM_NAMES = Set.of(
            "AggregateError", "Array", "ArrayBuffer", "Atomics", "BigInt", "BigInt64Array", "BigUint64Array", "Boolean",
            "DataView", "Date", "Error", "EvalError", "FinalizationRegistry", "Float16Array", "Float32Array", "Float64Array", "Function",
            "Int16Array", "Int32Array", "Int8Array", "Intl", "Iterator", "JSON", "Map", "Math", "Number", "Object",
            "Promise", "Proxy", "RangeError", "ReferenceError", "Reflect", "RegExp", "Set", "SharedArrayBuffer",
            "String", "Symbol", "SyntaxError", "Temporal", "TypeError", "URIError", "Uint16Array", "Uint32Array",
            "Uint8Array", "Uint8ClampedArray", "WeakMap", "WeakRef", "WeakSet");

    private static final Set<String> SOURCE_EXTENSIONS = Set.of("ts", "tsx", "js", "jsx", "mjs", "cjs", "mts", "cts", "vue", "svelte");
    private static final String SKIPPED_DIRECTORY = "node_modules";
    private static final Pattern DECLARATION = Pattern.compile(
            "\\b(?:(?:class|interface|type|enum|namespace|const|let|var)\\s+|function(?:\\s*\\*\\s*|\\s+))(?=([A-Za-z_$][A-Za-z0-9_$]*))");

    private final Set<String> declaredNames;

    private KnownFrontendSymbols(Set<String> declaredNames) {
        this.declaredNames = declaredNames;
    }

    static KnownFrontendSymbols fromSourceRoot(Path projectDirectory, String sourceRoot) {
        Set<String> declaredNames = new HashSet<>();
        if (!sourceRoot.isEmpty()) {
            Path root = projectDirectory.resolve(sourceRoot);
            if (!Files.isDirectory(root)) {
                throw new IllegalStateException("the frontend source root is no directory: " + sourceRoot);
            }
            collectNamesUnder(realPathOf(root), declaredNames);
        }
        return new KnownFrontendSymbols(declaredNames);
    }

    private static Path realPathOf(Path root) {
        try {
            return root.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void collectNamesUnder(Path root, Set<String> declaredNames) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    boolean skipped = directory.getFileName() != null && directory.getFileName().toString().equals(SKIPPED_DIRECTORY);
                    return skipped ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    String fileName = file.getFileName().toString();
                    if (attributes.isRegularFile() && SOURCE_EXTENSIONS.contains(extensionOf(fileName))) {
                        declaredNames.add(baseNameOf(fileName));
                        collectDeclarationsIn(file, declaredNames);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void collectDeclarationsIn(Path file, Set<String> declaredNames) throws IOException {
        String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Matcher declarations = DECLARATION.matcher(source);
        while (declarations.find()) {
            declaredNames.add(declarations.group(1));
        }
    }

    private static String extensionOf(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot < 0 ? "" : fileName.substring(lastDot + 1);
    }

    private static String baseNameOf(String fileName) {
        int firstDot = fileName.indexOf('.');
        return firstDot < 0 ? fileName : fileName.substring(0, firstDot);
    }

    boolean hasName(String name) {
        return declaredNames.contains(name) || PLATFORM_NAMES.contains(name);
    }
}
