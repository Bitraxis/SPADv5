package main;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class DragonToolkit {
    private static final Set<String> ENABLED_PACKAGES = new LinkedHashSet<>();
    private static final Set<String> EXTENSION_LANGUAGES = new LinkedHashSet<>();
    private static final List<String> REPOSITORIES = new ArrayList<>();

    private static String languageName = "SPAD";
    private static String spadVersion = "0.0.0";

    static {
        REPOSITORIES.add("ftp://central");
        REPOSITORIES.add("https://repo.spad-lang.org/ftp");
        EXTENSION_LANGUAGES.add("spad");
        EXTENSION_LANGUAGES.add("java");
        EXTENSION_LANGUAGES.add("lua");
    }

    private DragonToolkit() {}

    public static void bootstrap(String language, String version) {
        languageName = language;
        spadVersion = version;
    }

    public static String getCompilerProjectName() {
        return "dragon";
    }

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

    public static void addRepository(String repoUrl) {
        if (repoUrl != null && !repoUrl.isBlank()) {
            REPOSITORIES.add(repoUrl);
        }
    }

    public static void usePackages(List<String> packageNames) {
        for (String packageName : packageNames) {
            if (packageName != null && !packageName.isBlank()) {
                ENABLED_PACKAGES.add(packageName);
            }
        }
    }

    public static void importModule(String moduleName, String versionConstraint, String source) {
        String resolvedSource = (source == null || source.isBlank()) ? REPOSITORIES.get(0) : source;
        String resolvedVersion = (versionConstraint == null || versionConstraint.isBlank()) ? "*" : versionConstraint;
        ENABLED_PACKAGES.add(moduleName + "@" + resolvedVersion + "@" + resolvedSource);
    }

    public static List<String> getEnabledPackages() {
        return List.copyOf(ENABLED_PACKAGES);
    }
}
