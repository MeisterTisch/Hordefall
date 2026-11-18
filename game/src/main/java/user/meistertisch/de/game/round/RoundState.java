package user.meistertisch.de.game.round;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum RoundState {
    PREPARING,  // Preparing the world and server before a new player can start the round
    WAITING,    // Waiting for a new player to join
    COUNTDOWN,  // Starting the round
    RUNNING,    // Active round running
    LEVEL_UP,   // Active round: level_up phase
    PAUSED,     // Active round paused
    ENDING;     // Active round ending, cleaning up

    public static List<String> getListOfStates(){
        List<String> list = new ArrayList<>();
        Arrays.stream(RoundState.values()).forEach(state -> list.add(state.name()));
        return list;
    }
}
