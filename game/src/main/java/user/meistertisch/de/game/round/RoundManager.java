package user.meistertisch.de.game.round;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import user.meistertisch.de.game.Game;
import user.meistertisch.de.game.round.arena.ArenaTemplate;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RoundManager {
    private final HashMap<UUID, Round> activeRounds;
    private final Set<UUID> debugViewers;
    private BukkitTask tickTask;

    public RoundManager() {
        activeRounds = new HashMap<>();
        debugViewers = new HashSet<>();
    }

    // Tick Task
    public void startTickTask() {
        if (tickTask != null) return;

        tickTask = Game.getPlugin().getServer().getScheduler().runTaskTimer(Game.getPlugin(), () -> {
            if (activeRounds.isEmpty()) return;

            for (Round round : List.copyOf(activeRounds.values())) {
                try {
                    if (round.getState() == RoundState.ENDING) {
                        activeRounds.remove(round.getOwner());
                        continue;
                    }

                    round.tick();
                } catch (Exception e) {
                    Game.getPlugin().getLogger().log(Level.SEVERE, "Error ticking round for " + round.getOwner(), e);
                }
            }
        }, 0, 1);
    }

    public void stopTickTask() {
        if (tickTask != null) {
            Game.getPlugin().getLogger().log(Level.INFO, "Stopping Tick Task.");
            tickTask.cancel();
            tickTask = null;
        }
        activeRounds.values().forEach(round -> round.transitionTo(RoundState.ENDING));
        activeRounds.clear();
    }


    // Register
    public void registerRound(Player owner, ArenaTemplate arena) {
        Game.getPlugin().getLogger().log(Level.INFO, String.format("Registering new round for player '%s' with arena '%s'.", owner.getName(), arena.id()));

        Round round = new Round(owner.getUniqueId());
        activeRounds.put(owner.getUniqueId(), round);
        // Runs only once the world is 100% loaded on the main thread!
        round.prepare(Game.getPlugin(), arena)
                .thenAccept(owner::teleport)
                .exceptionally(e -> {
                    owner.sendMessage("Failed to load arena '" + arena + "'!");
                    Game.getPlugin().getLogger().log(Level.SEVERE, "Couldn't generate round for '" + owner.getName() + "' with '" + arena.id() + "' due to this error:");
                    e.printStackTrace();
                    this.unregisterRound(owner.getUniqueId());
                    return null;
                });
        Game.getPlugin().getLogger().log(Level.INFO, String.format("Round created for Player '%s' with arena '%s'!", owner.getName(), arena.id()));
    }

    public void unregisterRound(UUID owner) {
        Game.getPlugin().getLogger().log(Level.INFO, String.format("Unregistering round for UUID '%s'.", owner));

        Round round = activeRounds.remove(owner);
        if (round != null) {
            round.transitionTo(RoundState.ENDING);
        }
    }


    // Getter
    @Nullable
    public Round getRound(UUID owner) {
        return activeRounds.get(owner);
    }

    public boolean hasRound(UUID owner) {
        return activeRounds.containsKey(owner);
    }


    // Debug
    public boolean addDebugViewer(UUID viewer) {
        return debugViewers.add(viewer);
    }

    public boolean removeDebugViewer(UUID viewer) {
        return debugViewers.remove(viewer);
    }

    public boolean isDebugViewer(UUID viewer) {
        return debugViewers.contains(viewer);
    }

    public Set<UUID> getDebugViewers() {
        return Set.copyOf(debugViewers);
    }
}
