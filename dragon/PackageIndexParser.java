package dragon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class PackageIndexParser {
    public List<PackageRecord> parse(Path packagesToml) throws IOException {
        List<PackageRecord> out = new ArrayList<>();
        if (!Files.exists(packagesToml)) {
            return out;
        }

        String name = null;
        String set = null;
        String version = null;
        String entry = null;
        String kind = "spad";

        for (String raw : Files.readAllLines(packagesToml)) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("[[package]]")) {
                if (name != null && set != null && version != null && entry != null) {
                    out.add(new PackageRecord(name, set, version, entry, kind));
                }
                name = null;
                set = null;
                version = null;
                entry = null;
                kind = "spad";
                continue;
            }

            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = unquote(line.substring(eq + 1).trim());

            if ("name".equals(key)) {
                name = value;
            } else if ("set".equals(key)) {
                set = value;
            } else if ("version".equals(key)) {
                version = value;
            } else if ("entry".equals(key)) {
                entry = value;
            } else if ("kind".equals(key)) {
                kind = value;
            }
        }

        if (name != null && set != null && version != null && entry != null) {
            out.add(new PackageRecord(name, set, version, entry, kind));
        }

        return out;
    }

    private String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash >= 0) {
            return line.substring(0, hash);
        }
        return line;
    }

    private String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
