package dragon;

class DragonImport {
    public final String module;
    public final String versionConstraint;
    public final String source;

    public DragonImport(String module, String versionConstraint, String source) {
        this.module = module;
        this.versionConstraint = versionConstraint;
        this.source = source;
    }

    @Override
    public String toString() {
        return module + " " + versionConstraint + " from " + source;
    }
}
