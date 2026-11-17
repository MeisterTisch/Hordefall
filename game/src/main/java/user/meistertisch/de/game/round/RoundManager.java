package user.meistertisch.de.game.round;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import user.meistertisch.de.game.Game;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class RoundManager {
    private final HashMap<UUID, Round> activeRounds;
    private BukkitTask tickTask;

    public RoundManager(){
        activeRounds = new HashMap<>();
    }

    public void startTickTask(){
        if(tickTask != null) return;

        tickTask = Game.getPlugin().getServer().getScheduler().runTaskTimer(Game.getPlugin(), () -> {
            if(activeRounds.isEmpty()) return;

            for (Round round : List.copyOf(activeRounds.values())){
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

    public void stopTickTask(){
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        activeRounds.values().forEach(round -> round.transitionTo(RoundState.ENDING));
        activeRounds.clear();
    }

    public void registerRound(Player owner, String arena){
        Round round = new Round(owner.getUniqueId());
        activeRounds.put(owner.getUniqueId(), round);
        round.prepare(Game.getPlugin(), arena).thenRun(() -> {
            // Runs only once the world is 100% loaded on the main thread!
            owner.teleport(new Location(round.getWorld(), 7.5, 0.2, 22.5, 180, 0));
        }).exceptionally(_ -> {
            owner.sendMessage("Failed to load arena '" + arena + "'!");
            this.unregisterRound(owner.getUniqueId());
            return null;
        });
    }

    public void unregisterRound(UUID owner){
        Round round = activeRounds.remove(owner);
        if (round != null) {
            round.transitionTo(RoundState.ENDING);
        }
    }

    @Nullable
    public Round getRound(UUID owner){
        return activeRounds.get(owner);
    }

    public boolean hasRound(UUID owner) {
        return activeRounds.containsKey(owner);
    }
}
