package user.meistertisch.de.game.round.arena;

import java.nio.file.Path;
import java.util.Objects;

public record ArenaTemplate(String id, Path worldDir, ArenaSpawn spawn) {

    public ArenaTemplate {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(worldDir, "worldDir must not be null");
        Objects.requireNonNull(spawn, "spawn must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
