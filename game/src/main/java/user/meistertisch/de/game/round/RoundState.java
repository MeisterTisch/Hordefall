package user.meistertisch.de.game.round;

public enum RoundState {
    WAITING,    // Waiting for a new player to join
    COUNTDOWN,  // Starting the round
    RUNNING,    // Active round running
    LEVEL_UP,   // Active round: level_up phase
    PAUSED,     // Active round paused
    ENDING      // Active round ending, cleaning up
}
