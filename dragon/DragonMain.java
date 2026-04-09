package dragon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DragonMain {
    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception ex) {
            System.err.println("dragon :: error: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        if (args.length == 0 || isHelpCommand(args[0])) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase(Locale.ROOT);
        CliOptions options = parseOptions(Arrays.copyOfRange(args, 1, args.length));

        switch (command) {
            case "install-spad":
                installSpad(options.workspace);
                return;
            case "resolve":
                resolveImports(options.workspace, options.sourceFile, options.verbose);
                return;
            case "compile":
                compileSpad(options, false);
                return;
            case "package":
                compileSpad(options, true);
                return;
            case "list-packages":
                listPackages(options.workspace);
                return;
            default:
                throw new IllegalArgumentException("Unknown command: " + command + " (run 'dragon help')");
        }
    }

    private static void installSpad(Path workspace) throws IOException {
        Path marker = workspace.resolve(".dragon").resolve("spad-installed.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "SPAD runtime installed by dragon\n");
        System.out.println("dragon :: installed SPAD runtime marker at " + marker);
    }

    private static void compileSpad(CliOptions options, boolean forceJre) throws Exception {
        System.out.println("dragon :: compile requested for " + options.sourceFile);
        resolveImports(options.workspace, options.sourceFile, options.verbose);

        List<String> compilerArgs = new ArrayList<>();
        compilerArgs.add(options.sourceFile.toString());
        compilerArgs.add("--out");
        compilerArgs.add(options.outDir.toString());

        if (options.className != null && !options.className.isBlank()) {
            compilerArgs.add("--class");
            compilerArgs.add(options.className);
        }
        if (options.runCompiledClass) {
            compilerArgs.add("--run");
        }

        boolean emitJre = forceJre || options.emitJreBundle;
        if (emitJre) {
            compilerArgs.add("--jre");
            if (options.jreOutputPath != null) {
                compilerArgs.add("--jre-out");
                compilerArgs.add(options.jreOutputPath.toString());
            }
        }

        invokeSpadCompiler(options.workspace, compilerArgs);
    }

    private static void resolveImports(Path workspace, Path spadSource, boolean verbose) throws IOException {
        Path spadToml = workspace.resolve("spad.toml");
        Path dragonLock = workspace.resolve("dragon.lock");

        DragonTomlParser parser = new DragonTomlParser();
        DragonConfig config = parser.parseConfig(spadToml);
        DragonLock lock = parser.parseLock(dragonLock);

        SpadImportScanner scanner = new SpadImportScanner();
        List<DragonImport> imports = scanner.scan(spadSource);

        RepoResolver resolver = new RepoResolver();
        List<String> resolved = resolver.resolve(workspace, imports, config, lock);

        if (verbose) {
            System.out.println("dragon :: language=" + config.language + " version=" + config.version);
            System.out.println("dragon :: extension-languages=" + config.extensionLanguages);
            System.out.println("dragon :: repos=" + config.repos);
            System.out.println("dragon :: local-sources=" + config.localPackageDirs);
            System.out.println("dragon :: package-sets=" + config.packageSets);
            System.out.println("dragon :: cache=" + workspace.resolve(".dragon").resolve("cache"));
        }
        System.out.println("dragon :: imports from " + spadSource + ":");
        for (String line : resolved) {
            System.out.println("  - " + line);
        }
    }

    private static void listPackages(Path workspace) throws IOException {
        DragonTomlParser parser = new DragonTomlParser();
        DragonConfig config = parser.parseConfig(workspace.resolve("spad.toml"));
        PackageIndexParser packageIndexParser = new PackageIndexParser();

        Map<String, Integer> countByKind = new LinkedHashMap<>();
        int total = 0;
        for (String source : config.localPackageDirs) {
            Path sourceRoot = workspace.resolve(source);
            Path indexPath = sourceRoot.resolve("PACKAGES.toml");
            List<PackageRecord> records = packageIndexParser.parse(indexPath);
            if (records.isEmpty()) {
                continue;
            }

            System.out.println("dragon :: packages from " + source + ":");
            for (PackageRecord r : records) {
                System.out.println("  - " + r.name + "@" + r.version + " [set=" + r.set + ", kind=" + r.kind + ", entry=" + r.entry + "]");
                countByKind.put(r.kind, countByKind.getOrDefault(r.kind, 0) + 1);
                total++;
            }
        }

        System.out.println("dragon :: package count=" + total + " by-kind=" + countByKind);
    }

    private static void invokeSpadCompiler(Path workspace, List<String> compilerArgs) throws Exception {
        String javaBin = resolveJavaBinary();
        String classPath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classPath);
        command.add("main.main");
        command.addAll(compilerArgs);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workspace.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (InputStream stream = process.getInputStream()) {
            stream.transferTo(System.out);
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("SPAD compiler exited with code " + exit);
        }
    }

    private static String resolveJavaBinary() {
        String exe = isWindows() ? "java.exe" : "java";
        return Paths.get(System.getProperty("java.home"), "bin", exe).toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static CliOptions parseOptions(String[] args) {
        Path workspace = Path.of(".");
        Path source = null;
        Path outDir = Path.of("build", "spad");
        String className = null;
        boolean run = false;
        boolean jre = false;
        Path jreOut = null;
        boolean verbose = false;

        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if ("--workspace".equals(arg) || "-w".equals(arg)) {
                i = requireValueIndex(args, i, arg);
                workspace = Path.of(args[i]);
            } else if ("--source".equals(arg) || "-s".equals(arg)) {
                i = requireValueIndex(args, i, arg);
                source = Path.of(args[i]);
            } else if ("--out".equals(arg) || "-o".equals(arg)) {
                i = requireValueIndex(args, i, arg);
                outDir = Path.of(args[i]);
            } else if ("--class".equals(arg) || "-c".equals(arg)) {
                i = requireValueIndex(args, i, arg);
                className = args[i];
            } else if ("--run".equals(arg)) {
                run = true;
            } else if ("--jre".equals(arg)) {
                jre = true;
            } else if ("--jre-out".equals(arg)) {
                i = requireValueIndex(args, i, arg);
                jre = true;
                jreOut = Path.of(args[i]);
            } else if ("--verbose".equals(arg) || "-v".equals(arg)) {
                verbose = true;
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Unknown option: " + arg);
            } else {
                if (workspace.equals(Path.of("."))) {
                    workspace = Path.of(arg);
                } else if (source == null) {
                    source = Path.of(arg);
                } else {
                    throw new IllegalArgumentException("Unexpected positional argument: " + arg);
                }
            }
            i++;
        }

        workspace = workspace.toAbsolutePath().normalize();

        if (source == null) {
            source = workspace.resolve("main").resolve("main.spad");
        } else if (!source.isAbsolute()) {
            source = workspace.resolve(source);
        }
        source = source.toAbsolutePath().normalize();

        if (!outDir.isAbsolute()) {
            outDir = workspace.resolve(outDir);
        }
        outDir = outDir.toAbsolutePath().normalize();

        if (jreOut != null && !jreOut.isAbsolute()) {
            jreOut = workspace.resolve(jreOut);
        }
        if (jreOut != null) {
            jreOut = jreOut.toAbsolutePath().normalize();
        }

        return new CliOptions(workspace, source, outDir, className, run, jre, jreOut, verbose);
    }

    private static int requireValueIndex(String[] args, int currentIndex, String option) {
        int valueIndex = currentIndex + 1;
        if (valueIndex >= args.length) {
            throw new IllegalArgumentException("Missing value for option " + option);
        }
        return valueIndex;
    }

    private static boolean isHelpCommand(String arg) {
        return "help".equals(arg) || "--help".equals(arg) || "-h".equals(arg);
    }

    private static void printUsage() {
        System.out.println("dragon :: SPAD manager CLI");
        System.out.println("Usage: java dragon.DragonMain <command> [options]");
        System.out.println("Commands:");
        System.out.println("  resolve       Resolve imports for a source file");
        System.out.println("  compile       Resolve + compile one .spad file to classes (optional --run, --jre)");
        System.out.println("  package       Resolve + compile + emit .jre bundle for one .spad file");
        System.out.println("  list-packages List packages from configured local package directories");
        System.out.println("  install-spad  Install local runtime marker");
        System.out.println("Options:");
        System.out.println("  --workspace, -w <dir>   Workspace root (default .)");
        System.out.println("  --source, -s <file>     Input .spad source (default main/main.spad)");
        System.out.println("  --out, -o <dir>         Build output directory (default build/spad)");
        System.out.println("  --class, -c <name>      Generated JVM class name");
        System.out.println("  --run                   Run compiled main class after build");
        System.out.println("  --jre                   Emit .jre bundle");
        System.out.println("  --jre-out <file.jre>    Custom .jre output file path");
        System.out.println("  --verbose, -v           Print resolver configuration details");
        System.out.println("Legacy positional mode still works: java dragon.DragonMain resolve . main/main.spad");
    }

    private static final class CliOptions {
        private final Path workspace;
        private final Path sourceFile;
        private final Path outDir;
        private final String className;
        private final boolean runCompiledClass;
        private final boolean emitJreBundle;
        private final Path jreOutputPath;
        private final boolean verbose;

        private CliOptions(
                Path workspace,
                Path sourceFile,
                Path outDir,
                String className,
                boolean runCompiledClass,
                boolean emitJreBundle,
                Path jreOutputPath,
                boolean verbose
        ) {
            this.workspace = workspace;
            this.sourceFile = sourceFile;
            this.outDir = outDir;
            this.className = className;
            this.runCompiledClass = runCompiledClass;
            this.emitJreBundle = emitJreBundle;
            this.jreOutputPath = jreOutputPath;
            this.verbose = verbose;
        }
    }
}
