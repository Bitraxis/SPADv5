package main;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class main {
    private static final String DEFAULT_PACKAGE = "main";

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception ex) {
            System.err.println("SPAD compile failed: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        if (args.length == 0 || hasArg(args, "--help") || hasArg(args, "-h")) {
            printUsage();
            return;
        }

        CliOptions options = parseOptions(args);
        String source = Files.readString(options.inputFile);
        CompileResult result = compileSource(source, options.className, options.outDir, options.inputFile);

        System.out.println("Generated Java: " + result.generatedJavaPath.toAbsolutePath());
        if (options.printJava) {
            System.out.println(result.generatedJava);
        }

        System.out.println("Compiled classes: " + result.classOutputDir.toAbsolutePath());

        if (options.emitJreBundle) {
            Path jreBundle = options.jreOutputPath;
            if (jreBundle == null) {
                jreBundle = options.outDir.resolve(result.className + ".jre");
            }
            Path created = createJreBundle(result.classOutputDir, result.packageName, result.className, jreBundle);
            System.out.println("JRE bundle: " + created.toAbsolutePath());
        }

        if (options.runCompiledClass) {
            runCompiledMain(result.classOutputDir, result.packageName, result.className);
        }
    }

    private static CompileResult compileSource(
            String source,
            String className,
            Path outDir,
            Path inputFile
    ) throws IOException {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.lex();

        Parser parser = new Parser(tokens);
        Program program = parser.parseProgram();

        StaticTypeChecker typeChecker = new StaticTypeChecker();
        typeChecker.check(program);

        JavaEmitter emitter = new JavaEmitter();
        String javaCode = emitter.emitProgram(program, className, DEFAULT_PACKAGE);

        Path generatedSrcRoot = outDir.resolve("generated-src");
        Path generatedJava = generatedSrcRoot.resolve(DEFAULT_PACKAGE).resolve(className + ".java");
        Files.createDirectories(generatedJava.getParent());
        Files.writeString(generatedJava, javaCode);

        Path classOutputDir = outDir.resolve("classes");
        Files.createDirectories(classOutputDir);
        compileJavaSources(List.of(generatedJava), classOutputDir, inputFile);

        return new CompileResult(className, DEFAULT_PACKAGE, generatedJava, classOutputDir, javaCode);
    }

    private static void compileJavaSources(List<Path> generatedSources, Path classOutputDir, Path inputFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler not found. Run this with a JDK (not a JRE).");
        }

        Path projectRoot = findProjectRoot(inputFile.toAbsolutePath().getParent());
        if (projectRoot == null) {
            throw new IllegalStateException("Could not find project root containing main/SpadPrelude.java.");
        }

        List<Path> allSources = new ArrayList<>();
        allSources.add(projectRoot.resolve("main").resolve("SpadPrelude.java"));
        allSources.add(projectRoot.resolve("main").resolve("DragonToolkit.java"));
        allSources.addAll(generatedSources);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(allSources);
            List<String> options = List.of("-d", classOutputDir.toString());

            JavaCompiler.CompilationTask task = compiler.getTask(new PrintWriter(System.err), fileManager, diagnostics, options, null, units);
            boolean success = Boolean.TRUE.equals(task.call());
            if (!success) {
                StringBuilder message = new StringBuilder("javac failed:\n");
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    message.append("- ").append(d.getKind())
                            .append(" at ")
                            .append(d.getSource() == null ? "<unknown>" : d.getSource().toUri())
                            .append(":")
                            .append(d.getLineNumber())
                            .append(" -> ")
                            .append(d.getMessage(Locale.getDefault()))
                            .append("\n");
                }
                throw new IllegalStateException(message.toString());
            }
        }
    }

    private static Path findProjectRoot(Path startDir) {
        Path current = startDir;
        while (current != null) {
            if (Files.exists(current.resolve("main").resolve("SpadPrelude.java"))
                    && Files.exists(current.resolve("main").resolve("DragonToolkit.java"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static void runCompiledMain(Path classOutputDir, String packageName, String className) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classOutputDir.toUri().toURL()})) {
            String fqcn = packageName + "." + className;
            Class<?> generatedClass = Class.forName(fqcn, true, loader);
            Method method = generatedClass.getMethod("main", String[].class);
            System.out.println("Running " + fqcn + ".main() ...");
            method.invoke(null, (Object) new String[0]);
        }
    }

    private static Path createJreBundle(Path classOutputDir, String packageName, String className, Path outputFile) throws IOException {
        String mainClass = packageName + "." + className;
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);

        Path parent = outputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (JarOutputStream jar = new JarOutputStream(
                Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                manifest
        )) {
            List<Path> classFiles = new ArrayList<>();
            try (var stream = Files.walk(classOutputDir)) {
                stream.filter(Files::isRegularFile).forEach(classFiles::add);
            }

            for (Path classFile : classFiles) {
                String entryName = classOutputDir.relativize(classFile).toString().replace('\\', '/');
                if ("META-INF/MANIFEST.MF".equalsIgnoreCase(entryName)) {
                    continue;
                }
                jar.putNextEntry(new JarEntry(entryName));
                Files.copy(classFile, jar);
                jar.closeEntry();
            }
        }

        return outputFile;
    }

    private static CliOptions parseOptions(String[] args) {
        Path inputFile = null;
        Path outDir = Paths.get("build", "spad");
        String className = null;
        boolean runCompiledClass = false;
        boolean printJava = false;
        boolean emitJreBundle = false;
        Path jreOutputPath = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--out":
                case "-o":
                    i = ensureValue(args, i, arg);
                    outDir = Paths.get(args[i]);
                    break;
                case "--class":
                case "-c":
                    i = ensureValue(args, i, arg);
                    className = sanitizeClassName(args[i]);
                    break;
                case "--run":
                    runCompiledClass = true;
                    break;
                case "--print-java":
                    printJava = true;
                    break;
                case "--jre":
                    emitJreBundle = true;
                    break;
                case "--jre-out":
                    i = ensureValue(args, i, arg);
                    emitJreBundle = true;
                    jreOutputPath = Paths.get(args[i]);
                    break;
                default:
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    if (inputFile != null) {
                        throw new IllegalArgumentException("Only one input .spad file is supported per invocation.");
                    }
                    inputFile = Paths.get(arg);
            }
        }

        if (inputFile == null) {
            throw new IllegalArgumentException("Missing input .spad file.");
        }
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Input file not found: " + inputFile);
        }
        if (!inputFile.toString().endsWith(".spad")) {
            throw new IllegalArgumentException("Input must be a .spad file: " + inputFile);
        }

        if (className == null) {
            className = sanitizeClassName(stripExtension(inputFile.getFileName().toString()));
        }

        return new CliOptions(inputFile, outDir, className, runCompiledClass, printJava, emitJreBundle, jreOutputPath);
    }

    private static int ensureValue(String[] args, int currentIndex, String option) {
        int valueIndex = currentIndex + 1;
        if (valueIndex >= args.length) {
            throw new IllegalArgumentException("Missing value for option: " + option);
        }
        return valueIndex;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    private static String sanitizeClassName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "SpadGenerated";
        }

        StringBuilder out = new StringBuilder();
        char first = rawName.charAt(0);
        if (!Character.isJavaIdentifierStart(first)) {
            out.append('S');
        }
        for (int i = 0; i < rawName.length(); i++) {
            char c = rawName.charAt(i);
            out.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
        return out.toString();
    }

    private static boolean hasArg(String[] args, String value) {
        for (String arg : args) {
            if (value.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("SPAD JVM compiler");
        System.out.println("Usage: java main.main <file.spad> [--out <dir>] [--class <ClassName>] [--run] [--print-java] [--jre] [--jre-out <file.jre>]");
    }

    private static final class CliOptions {
        private final Path inputFile;
        private final Path outDir;
        private final String className;
        private final boolean runCompiledClass;
        private final boolean printJava;
        private final boolean emitJreBundle;
        private final Path jreOutputPath;

        private CliOptions(
                Path inputFile,
                Path outDir,
                String className,
                boolean runCompiledClass,
                boolean printJava,
                boolean emitJreBundle,
                Path jreOutputPath
        ) {
            this.inputFile = inputFile;
            this.outDir = outDir;
            this.className = className;
            this.runCompiledClass = runCompiledClass;
            this.printJava = printJava;
            this.emitJreBundle = emitJreBundle;
            this.jreOutputPath = jreOutputPath;
        }
    }

    private static final class CompileResult {
        private final String className;
        private final String packageName;
        private final Path generatedJavaPath;
        private final Path classOutputDir;
        private final String generatedJava;

        private CompileResult(
                String className,
                String packageName,
                Path generatedJavaPath,
                Path classOutputDir,
                String generatedJava
        ) {
            this.className = className;
            this.packageName = packageName;
            this.generatedJavaPath = generatedJavaPath;
            this.classOutputDir = classOutputDir;
            this.generatedJava = generatedJava;
        }
    }
}