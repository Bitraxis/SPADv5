package dragon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ProjectionProfileEngine {
    private static final Pattern PROJECT_PATTERN = Pattern.compile("^\\s*project\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\{.*$");
    private static final Pattern PROJECTION_PATTERN = Pattern.compile("^\\s*projection\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");

    public List<ProjectionProfile> scan(Path spadFile) throws IOException {
        List<ProjectionProfile> profiles = new ArrayList<>();
        List<String> lines = Files.readAllLines(spadFile);

        String currentProject = null;
        int projectDepth = 0;

        for (String raw : lines) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) {
                continue;
            }

            if (currentProject == null) {
                Matcher projectMatcher = PROJECT_PATTERN.matcher(line);
                if (projectMatcher.matches()) {
                    currentProject = projectMatcher.group(1);
                    projectDepth = braceDelta(line);
                    if (projectDepth <= 0) {
                        currentProject = null;
                    }
                    continue;
                }
            } else {
                Matcher projectionMatcher = PROJECTION_PATTERN.matcher(line);
                if (projectionMatcher.matches()) {
                    String name = projectionMatcher.group(1);
                    String rawValue = stripTrailingSeparator(projectionMatcher.group(2).trim());
                    profiles.add(new ProjectionProfile(currentProject, name, rawValue));
                }

                projectDepth += braceDelta(line);
                if (projectDepth <= 0) {
                    currentProject = null;
                    projectDepth = 0;
                }
            }
        }

        return profiles;
    }

    public ProjectionProfile resolveByName(List<ProjectionProfile> profiles, String projectionName) {
        for (ProjectionProfile p : profiles) {
            if (p.name.equals(projectionName)) {
                return p;
            }
        }
        return null;
    }

    private String stripComment(String line) {
        int idx = line.indexOf("//");
        if (idx >= 0) {
            return line.substring(0, idx);
        }
        return line;
    }

    private String stripTrailingSeparator(String value) {
        String out = value;
        while (out.endsWith(";") || out.endsWith(",")) {
            out = out.substring(0, out.length() - 1).trim();
        }
        return out;
    }

    private int braceDelta(String line) {
        int delta = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '{') {
                delta++;
            } else if (c == '}') {
                delta--;
            }
        }
        return delta;
    }

    static final class ProjectionProfile {
        final String project;
        final String name;
        final String rawValue;

        ProjectionProfile(String project, String name, String rawValue) {
            this.project = project;
            this.name = name;
            this.rawValue = rawValue;
        }

        String normalizedValue() {
            String value = rawValue == null ? "" : rawValue.trim();
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }
    }
}
