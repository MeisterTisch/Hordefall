package user.meistertisch.de.game.round;

import java.util.UUID;

public class Round {
    private UUID owner;
    private RoundState state = RoundState.WAITING;
    private long tickCounter = 0;

    public Round(UUID owner) {
        this.owner = owner;
    }

    void tick() {
        // Only advance the run timer while actively running (freeze on LEVEL_UP or PAUSED)
        if (state != RoundState.RUNNING) {
            return;
        }

        tickCounter++;

        // Optional: check if the 30-minute run limit (36,000 ticks) is reached[cite: 5, 34]
        if (tickCounter >= 5 * 60 * 20) { // minutes * 60 seconds * 20 ticks/second
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

    private void cleanup() {
        // 1. Restore player state (speed, health scale, clear inventory/effects)
        // 2. Remove all spawned monsters, projectiles, and display entities (Milestone 2+)[cite: 37]
        // 3. Clear internal maps/lists so nothing leaks[cite: 37]
    }

    // GETTER
    public RoundState getState() {
        return state;
    }

    public UUID getOwner() {
        return owner;
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

    public void setOwner(UUID owner) {
        this.owner = owner;
    }
}
