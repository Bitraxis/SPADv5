package dragon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DragonMain {
    public static void main(String[] args) throws IOException {
        String command = args.length > 0 ? args[0] : "resolve";
        Path workspace = args.length > 1 ? Path.of(args[1]) : Path.of(".");

        switch (command) {
            case "install-spad":
                installSpad(workspace);
                return;
            case "compile":
                Path compileSource = args.length > 2 ? workspace.resolve(args[2]) : workspace.resolve("main/main.spad");
                compileSpad(workspace, compileSource);
                return;
            case "resolve":
            default:
                Path resolveSource = args.length > 2 ? workspace.resolve(args[2]) : workspace.resolve("main/main.spad");
                resolveImports(workspace, resolveSource);
                return;
        }
    }

    private static void installSpad(Path workspace) throws IOException {
        Path marker = workspace.resolve(".dragon").resolve("spad-installed.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "SPAD runtime installed by dragon\n");
        System.out.println("dragon :: installed SPAD runtime marker at " + marker);
    }

    private static void compileSpad(Path workspace, Path spadSource) throws IOException {
        System.out.println("dragon :: compile requested for " + spadSource);
        resolveImports(workspace, spadSource);
        System.out.println("dragon :: compile step delegates to SPAD transpiler (java main.main <file>)");
    }

    private static void resolveImports(Path workspace, Path spadSource) throws IOException {
        Path spadToml = workspace.resolve("spad.toml");
        Path dragonLock = workspace.resolve("dragon.lock");

        DragonTomlParser parser = new DragonTomlParser();
        DragonConfig config = parser.parseConfig(spadToml);
        DragonLock lock = parser.parseLock(dragonLock);

        SpadImportScanner scanner = new SpadImportScanner();
        List<DragonImport> imports = scanner.scan(spadSource);

        RepoResolver resolver = new RepoResolver();
        List<String> resolved = resolver.resolve(workspace, imports, config, lock);

        System.out.println("dragon :: language=" + config.language + " version=" + config.version);
        System.out.println("dragon :: extension-languages=" + config.extensionLanguages);
        System.out.println("dragon :: repos=" + config.repos);
        System.out.println("dragon :: local-sources=" + config.localPackageDirs);
        System.out.println("dragon :: package-sets=" + config.packageSets);
        System.out.println("dragon :: cache=" + workspace.resolve(".dragon").resolve("cache"));
        System.out.println("dragon :: imports from " + spadSource + ":");
        for (String line : resolved) {
            System.out.println("  - " + line);
        }
    }
}
