package user.meistertisch.de.game.round;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import user.meistertisch.de.game.Game;

import java.util.List;
import java.util.UUID;

public class CommandRound implements TabExecutor {
    // TabExecutor
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(!(sender instanceof Player player)){
            sender.sendMessage("Only for players");
            return true;
        }

        if(!player.isOp()){
            player.sendMessage("Only for players with OP!");
            return true;
        }

        if(args.length == 0 || args.length > 2){
            player.sendMessage("Invalid Syntax!");
            return true;
        }

        switch (args[0].toLowerCase()){
            case "start" -> startRound(player, "arena1");
            case "stop" -> stopRound(player);
            case "state" -> {
                if(args.length == 1) getState(player);
                else switchState(player, args[1]);
            }
            case "debug" -> player.sendMessage("Debug todo");
            default -> player.sendMessage("Invalid Syntax!");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }


    // Subcommands
    private void startRound(Player owner, String arena){
        RoundManager manager = Game.getRoundManager();

        if(manager.hasRound(owner.getUniqueId())){
            owner.sendMessage("Active Round already open!");
            return;
        }

        owner.sendMessage("Registering new Round in arena '" + arena + "' for '" + owner.getName() + "'.");
        manager.registerRound(owner, arena);
    }

    private void stopRound(Player owner){
        RoundManager manager = Game.getRoundManager();

        if(!manager.hasRound(owner.getUniqueId())){
            owner.sendMessage("No active round open!");
            return;
        }

        owner.sendMessage("Stopping Round for '" + owner.getName() + "'.");
        manager.unregisterRound(owner.getUniqueId());
    }

    private void getState(Player owner){
        RoundManager manager = Game.getRoundManager();
        Round round = manager.getRound(owner.getUniqueId());

        if(!manager.hasRound(owner.getUniqueId()) || round == null){
            owner.sendMessage("No active round open!");
            return;
        }

        owner.sendMessage("State is '" + round.getState().name() + "' for Round for '" + owner.getName() + "'.");
    }

    private void switchState(Player owner, String stateString){
        RoundManager manager = Game.getRoundManager();
        Round round = manager.getRound(owner.getUniqueId());

        if(!manager.hasRound(owner.getUniqueId()) || round == null){
            owner.sendMessage("No active round open!");
            return;
        }

        stateString = stateString.toUpperCase();
        if(!RoundState.getListOfStates().contains(stateString)){
            owner.sendMessage("No such State!");
            return;
        }

        RoundState state = RoundState.valueOf(stateString);
        owner.sendMessage("Switching State from '" + round.getState().name() + "' to '" + stateString + "' for Round for '" + owner.getName() + "'.");
        round.transitionTo(state);
    }
}
