package user.meistertisch.de.game.round.arena;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public record TemplateScanResult(
        Map<String, ArenaTemplate> valid,
        Map<String, String> errors
) {

    public TemplateScanResult {
        valid = Collections.unmodifiableMap(new LinkedHashMap<>(valid));
        errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public boolean hasUsableArenas() {
        return !valid.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Optional<ArenaTemplate> find(String id) {
        return Optional.ofNullable(valid.get(id));
    }

    public Optional<ArenaTemplate> pickRandom() {
        if (valid.isEmpty()) {
            return Optional.empty();
        }
        List<ArenaTemplate> arenas = new ArrayList<>(valid.values());
        return Optional.of(arenas.get(ThreadLocalRandom.current().nextInt(arenas.size())));
    }
}
