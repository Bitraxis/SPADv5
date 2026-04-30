package main;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// DragonToolkit is the shared runtime state that SPAD-generated code uses for
// package registration, repository metadata, and simple bootstrap configuration.
public class DragonToolkit {
    private static final Set<String> ENABLED_PACKAGES = new LinkedHashSet<>();
    private static final Set<String> EXTENSION_LANGUAGES = new LinkedHashSet<>();
    private static final List<String> REPOSITORIES = new ArrayList<>();

    private static String languageName = "SPAD";
    private static String spadVersion = "0.0.0";

    // Default repositories and supported extension languages are seeded once at
    // load time.
    static {
        REPOSITORIES.add("ftp://central");
        REPOSITORIES.add("https://repo.spad-lang.org/ftp");
        EXTENSION_LANGUAGES.add("spad");
        EXTENSION_LANGUAGES.add("java");
        EXTENSION_LANGUAGES.add("lua");
    }

    // The toolkit is a static utility and should never be instantiated.
    private DragonToolkit() {
    }

    // Bootstrap records the active language name and version for emitted programs.
    public static void bootstrap(String language, String version) {
        languageName = language;
        spadVersion = version;
    }

    // The compiler project name is used by packaging and naming conventions.
    public static String getCompilerProjectName() {
        return "dragon";
    }

    // Accessors expose the current runtime metadata without allowing mutation.
    public static String getLanguageName() {
        return languageName;
    }

    public static String getSpadVersion() {
        return spadVersion;
    }

    public static List<String> getRepositories() {
        return List.copyOf(REPOSITORIES);
    }

    public static List<String> getExtensionLanguages() {
        return List.copyOf(EXTENSION_LANGUAGES);
    }

    // Repository additions extend the resolution search path at runtime.
    public static void addRepository(String repoUrl) {
        if (repoUrl != null && !repoUrl.isBlank()) {
            REPOSITORIES.add(repoUrl);
        }
    }

    // Package-use declarations are tracked so the runtime knows what the program
    // requested.
    public static void usePackages(List<String> packageNames) {
        for (String packageName : packageNames) {
            if (packageName != null && !packageName.isBlank()) {
                ENABLED_PACKAGES.add(packageName);
            }
        }
    }

    // Import metadata is normalized into a single string for later inspection.
    public static void importModule(String moduleName, String versionConstraint, String source) {
        String resolvedSource = (source == null || source.isBlank()) ? REPOSITORIES.get(0) : source;
        String resolvedVersion = (versionConstraint == null || versionConstraint.isBlank()) ? "*" : versionConstraint;
        ENABLED_PACKAGES.add(moduleName + "@" + resolvedVersion + "@" + resolvedSource);
    }

    // Callers receive a read-only view of the recorded package requests.
    public static List<String> getEnabledPackages() {
        return List.copyOf(ENABLED_PACKAGES);
    }
}
