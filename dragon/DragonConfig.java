package dragon;

import java.util.ArrayList;
import java.util.List;

class DragonConfig {
    public String language = "SPAD";
    public String version = "0.1.0";
    public final List<String> repos = new ArrayList<>();
    public final List<String> localPackageDirs = new ArrayList<>();
    public final List<String> packageSets = new ArrayList<>();
    public final List<String> extensionLanguages = new ArrayList<>();

    public DragonConfig() {
        repos.add("ftp://central");
        repos.add("https://repo.spad-lang.org/ftp");
        localPackageDirs.add("packages-local");
        packageSets.add("T");
        packageSets.add("S");
        packageSets.add("P");
        packageSets.add("A");
        packageSets.add("D");
        packageSets.add("L");
        extensionLanguages.add("spad");
        extensionLanguages.add("java");
        extensionLanguages.add("lua");
    }
}
