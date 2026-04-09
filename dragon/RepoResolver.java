package dragon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class RepoResolver {
    public List<String> resolve(Path workspace, List<DragonImport> imports, DragonConfig config, DragonLock lock) throws IOException {
        List<String> resolutions = new ArrayList<>();
        PackageIndexParser packageIndexParser = new PackageIndexParser();
        Path cacheRoot = workspace.resolve(".dragon").resolve("cache");
        Files.createDirectories(cacheRoot);

        for (DragonImport imp : imports) {
            String lockedSource = lock.pinnedSources.get(imp.module);
            String lockedVersion = lock.pinnedVersions.get(imp.module);

            Set<String> orderedSources = new LinkedHashSet<>();
            if (imp.source != null && !imp.source.isBlank()) {
                orderedSources.add(imp.source);
            }
            if (lockedSource != null && !lockedSource.isBlank()) {
                orderedSources.add(lockedSource);
            }
            orderedSources.addAll(config.repos);
            orderedSources.addAll(config.localPackageDirs);

            PackageRecord selected = null;
            String selectedSource = null;

            for (String source : orderedSources) {
                Path packagesToml = locatePackagesToml(workspace, source);
                List<PackageRecord> records = packageIndexParser.parse(packagesToml);
                PackageRecord candidate = selectBest(records, imp, lockedVersion, config.packageSets);
                if (candidate != null) {
                    selected = candidate;
                    selectedSource = source;
                    break;
                }
            }

            if (selected == null) {
                resolutions.add(imp.module + " => unresolved (constraint " + imp.versionConstraint + ")");
                continue;
            }

            String endpoint = normalizeRepo(selectedSource) + "/" + selected.set + "/" + selected.entry
                    + "?version=" + urlEncode(selected.version);
            String cached = cachePackage(workspace, cacheRoot, selectedSource, selected);
            resolutions.add(imp.module + " => " + endpoint + " [set=" + selected.set + ", cache=" + cached + "]");
        }

        return resolutions;
    }

    private PackageRecord selectBest(
            List<PackageRecord> records,
            DragonImport imp,
            String lockedVersion,
            List<String> allowedSets
    ) {
        PackageRecord lockedCandidate = null;
        PackageRecord best = null;
        Semver bestSemver = null;
        String constraint = imp.versionConstraint == null || imp.versionConstraint.isBlank() ? "*" : imp.versionConstraint;

        for (PackageRecord record : records) {
            if (!imp.module.equals(record.name)) {
                continue;
            }
            if (!allowedSets.contains(record.set)) {
                continue;
            }

            Semver version;
            try {
                version = Semver.parse(record.version);
            } catch (Exception ex) {
                continue;
            }

            if (!version.satisfies(constraint)) {
                continue;
            }

            if (lockedVersion != null && !lockedVersion.isBlank() && lockedVersion.equals(record.version)) {
                lockedCandidate = record;
            }

            if (best == null || version.compareTo(bestSemver) > 0) {
                best = record;
                bestSemver = version;
            }
        }

        return lockedCandidate != null ? lockedCandidate : best;
    }

    private Path locatePackagesToml(Path workspace, String source) {
        Path localRoot = resolveLocalRoot(workspace, source);
        if (localRoot != null) {
            return localRoot.resolve("PACKAGES.toml");
        }

        String safe = sanitize(source);
        return workspace.resolve(".dragon").resolve("cache").resolve("manifests").resolve(safe).resolve("PACKAGES.toml");
    }

    private String cachePackage(Path workspace, Path cacheRoot, String source, PackageRecord record) throws IOException {
        Path localRoot = resolveLocalRoot(workspace, source);
        Path destinationDir = cacheRoot.resolve("packages").resolve(record.name).resolve(record.version);
        Files.createDirectories(destinationDir);

        if (localRoot != null) {
            String normalizedEntry = record.entry.replace("/", java.io.File.separator).replace("\\", java.io.File.separator);
            Path sourcePath = localRoot.resolve(record.set).resolve(normalizedEntry);
            if (!Files.exists(sourcePath)) {
                sourcePath = localRoot.resolve(normalizedEntry);
            }
            if (Files.exists(sourcePath)) {
                Path destination = destinationDir.resolve(normalizedEntry);
                Files.createDirectories(destination.getParent());
                Files.copy(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING);
                Path marker = destinationDir.resolve("REMOTE_FETCH.todo");
                if (Files.exists(marker)) {
                    Files.delete(marker);
                }
                return workspace.relativize(destination).toString();
            }
        }

        Path marker = destinationDir.resolve("REMOTE_FETCH.todo");
        Files.writeString(marker, "pull from " + source + " set=" + record.set + " entry=" + record.entry + "\n");
        return workspace.relativize(marker).toString();
    }

    private Path resolveLocalRoot(Path workspace, String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        if (source.startsWith("ftp://") || source.startsWith("http://") || source.startsWith("https://")) {
            return null;
        }
        String value = source;
        if (value.startsWith("local://")) {
            value = value.substring("local://".length());
        }

        Path candidate = Path.of(value);
        if (!candidate.isAbsolute()) {
            candidate = workspace.resolve(value);
        }
        if (Files.isDirectory(candidate)) {
            return candidate;
        }
        return null;
    }

    private String sanitize(String source) {
        return source.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String normalizeRepo(String repo) {
        if (repo.startsWith("ftp://")) {
            String tail = repo.substring("ftp://".length());
            return "https://repo.spad-lang.org/" + tail;
        }
        return repo;
    }

    private String urlEncode(String value) {
        return value.replace(" ", "%20").replace(">", "%3E").replace("<", "%3C").replace("=", "%3D");
    }
}
