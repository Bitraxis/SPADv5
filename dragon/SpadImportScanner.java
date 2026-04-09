package dragon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SpadImportScanner {
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "^\\s*import\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:=\\s*\"([^\"]+)\")?\\s*(?:from\\s*(?:\"([^\"]+)\"|([a-zA-Z_][a-zA-Z0-9_:/.-]*)))?\\s*$"
    );

    public List<DragonImport> scan(Path spadFile) throws IOException {
        List<DragonImport> imports = new ArrayList<>();
        for (String rawLine : Files.readAllLines(spadFile)) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }
            Matcher m = IMPORT_PATTERN.matcher(line);
            if (!m.matches()) {
                continue;
            }

            String module = m.group(1);
            String version = m.group(2) == null ? "*" : m.group(2);
            String source = "ftp://central";
            if (m.group(3) != null) {
                source = m.group(3);
            } else if (m.group(4) != null) {
                source = m.group(4);
            }
            imports.add(new DragonImport(module, version, source));
        }
        return imports;
    }
}
