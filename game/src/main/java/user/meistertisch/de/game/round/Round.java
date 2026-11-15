package user.meistertisch.de.game.round;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class Round {
    private UUID playerOwner;
    private RoundState state = RoundState.WAITING;
    private long tickCounter = 0;

    public Round(UUID playerOwner) {
        this.playerOwner = playerOwner;
    }

    void tick() {
        // Only advance the run timer while actively running (freeze on LEVEL_UP or PAUSED)
        if (state != RoundState.RUNNING) {
            return;
        }

        tickCounter++;

        // Optional: check if the 30-minute run limit (36,000 ticks) is reached[cite: 5, 34]
        if (tickCounter >= 36_000L) {
            transitionTo(RoundState.ENDING);
        }
    }

    void transitionTo(RoundState next) {
        if (this.state == next || !isValidTransition(this.state, next)) {
            return; // Ignore duplicate or illegal transitions
        }

        RoundState previous = this.state;
        this.state = next;

        // 1. Handle leaving the previous state
        if (previous == RoundState.LEVEL_UP) {
            // TODO
//            unfreezePlayer(); // Restore movement & jump attributes
        }

        // 2. Handle entering the new state
        switch (next) {
            // TODO
//            case LEVEL_UP -> freezePlayer(); // Set speed & jump strength to 0
//            case ENDING -> cleanup();        // Cancel task, clear monsters/projectiles
            default -> {}
        }
    }

    private boolean isValidTransition(RoundState from, RoundState to) {
        return switch (from) {
            case WAITING   -> to == RoundState.COUNTDOWN || to == RoundState.RUNNING || to == RoundState.ENDING;
            case COUNTDOWN, LEVEL_UP, PAUSED -> to == RoundState.RUNNING || to == RoundState.ENDING;
            case RUNNING   -> to == RoundState.LEVEL_UP || to == RoundState.PAUSED || to == RoundState.ENDING;
            case ENDING    -> false; // Terminal state: once ending, never go back
        };
    }

    // GETTER
    public RoundState getState() {
        return state;
    }

    public UUID getPlayerOwner() {
        return playerOwner;
    }

    public long getTickCounter() {
        return tickCounter;
    }

    public int getElapsedSeconds() {
        return (int) (tickCounter / 20L);
    }


    // SETTER
    public void setState(RoundState state) {
        this.state = state;
    }

    public void setPlayerOwner(UUID playerOwner) {
        this.playerOwner = playerOwner;
    }
}
