package dragon;

import java.util.Objects;

class Semver implements Comparable<Semver> {
    public final int major;
    public final int minor;
    public final int patch;

    private Semver(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static Semver parse(String raw) {
        String[] parts = raw.trim().split("\\.");
        int major = parsePart(parts, 0);
        int minor = parsePart(parts, 1);
        int patch = parsePart(parts, 2);
        return new Semver(major, minor, patch);
    }

    public boolean satisfies(String constraint) {
        String c = constraint == null ? "*" : constraint.trim();
        if (c.isEmpty() || "*".equals(c)) {
            return true;
        }

        if (c.startsWith(">=")) {
            return compareTo(parse(c.substring(2))) >= 0;
        }
        if (c.startsWith("<=")) {
            return compareTo(parse(c.substring(2))) <= 0;
        }
        if (c.startsWith(">")) {
            return compareTo(parse(c.substring(1))) > 0;
        }
        if (c.startsWith("<")) {
            return compareTo(parse(c.substring(1))) < 0;
        }
        if (c.startsWith("^")) {
            Semver base = parse(c.substring(1));
            Semver ceiling = new Semver(base.major + 1, 0, 0);
            return compareTo(base) >= 0 && compareTo(ceiling) < 0;
        }
        if (c.startsWith("~")) {
            Semver base = parse(c.substring(1));
            Semver ceiling = new Semver(base.major, base.minor + 1, 0);
            return compareTo(base) >= 0 && compareTo(ceiling) < 0;
        }

        return compareTo(parse(c)) == 0;
    }

    private static int parsePart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        String token = parts[index].replaceAll("[^0-9]", "");
        if (token.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(token);
    }

    @Override
    public int compareTo(Semver other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Semver)) {
            return false;
        }
        Semver other = (Semver) o;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }
}
