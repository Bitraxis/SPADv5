package dragon;

class PackageRecord {
    public final String name;
    public final String set;
    public final String version;
    public final String entry;
    public final String kind;

    public PackageRecord(String name, String set, String version, String entry, String kind) {
        this.name = name;
        this.set = set;
        this.version = version;
        this.entry = entry;
        this.kind = kind;
    }
}
