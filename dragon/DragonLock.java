package dragon;

import java.util.LinkedHashMap;
import java.util.Map;

class DragonLock {
    public final Map<String, String> pinnedVersions = new LinkedHashMap<>();
    public final Map<String, String> pinnedSources = new LinkedHashMap<>();
}
