package user.meistertisch.de.game.round.arena;

import org.bukkit.Location;
import org.bukkit.World;

public record ArenaSpawn(double x, double y, double z, float yaw, float pitch) {
    public ArenaSpawn {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Spawn coordinates must be finite numbers");
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Spawn rotation must be finite numbers");
        }
    }

    public static ArenaSpawn of(double x, double y, double z) {
        return new ArenaSpawn(x, y, z, 0.0f, 0.0f);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
