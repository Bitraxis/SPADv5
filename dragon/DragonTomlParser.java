package dragon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class DragonTomlParser {
    public DragonConfig parseConfig(Path filePath) throws IOException {
        DragonConfig config = new DragonConfig();
        List<String> lines = Files.readAllLines(filePath);
        String section = "";

        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();

            if ("spad".equals(section)) {
                if ("language".equals(key)) {
                    config.language = unquote(value);
                } else if ("version".equals(key)) {
                    config.version = unquote(value);
                }
            } else if ("repos".equals(section) && "sources".equals(key)) {
                config.repos.clear();
                config.repos.addAll(parseStringArray(value));
            } else if ("repos".equals(section) && "local_sources".equals(key)) {
                config.localPackageDirs.clear();
                config.localPackageDirs.addAll(parseStringArray(value));
            } else if ("packages".equals(section) && "sets".equals(key)) {
                config.packageSets.clear();
                config.packageSets.addAll(parseStringArray(value));
            } else if ("extensions".equals(section) && "languages".equals(key)) {
                config.extensionLanguages.clear();
                config.extensionLanguages.addAll(parseStringArray(value));
            }
        }

        return config;
    }

    public DragonLock parseLock(Path filePath) throws IOException {
        DragonLock lock = new DragonLock();
        List<String> lines = Files.readAllLines(filePath);

        String currentModule = null;
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("[[module]]")) {
                currentModule = null;
                continue;
            }

            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }

            String key = line.substring(0, eq).trim();
            String value = unquote(line.substring(eq + 1).trim());

            if ("name".equals(key)) {
                currentModule = value;
            } else if ("version".equals(key) && currentModule != null) {
                lock.pinnedVersions.put(currentModule, value);
            } else if ("source".equals(key) && currentModule != null) {
                lock.pinnedSources.put(currentModule, value);
            }
        }

        return lock;
    }

    private String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash >= 0) {
            return line.substring(0, hash);
        }
        return line;
    }

    private List<String> parseStringArray(String value) {
        List<String> out = new ArrayList<>();
        String v = value.trim();
        if (!v.startsWith("[") || !v.endsWith("]")) {
            return out;
        }
        String inner = v.substring(1, v.length() - 1).trim();
        if (inner.isEmpty()) {
            return out;
        }
        String[] parts = inner.split(",");
        for (String part : parts) {
            out.add(unquote(part.trim()));
        }
        return out;
    }

    private String unquote(String value) {
        String v = value.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }
}
