package user.meistertisch.de.game.round;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import user.meistertisch.de.game.Game;
import user.meistertisch.de.game.round.arena.ArenaTemplate;
import user.meistertisch.de.game.round.arena.ArenaTemplateRegistry;
import user.meistertisch.de.game.round.arena.TemplateScanResult;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class CommandRound implements TabExecutor {
    // TabExecutor
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(!sender.isOp()){
            sender.sendMessage("Only for players with OP!");
            return true;
        }

        if(!(sender instanceof Player) && args.length == 1 && args[0].equalsIgnoreCase("reload")){
            reload(sender);
            return true;
        }

        if(!(sender instanceof Player player)){
            sender.sendMessage("Only for players");
            return true;
        }

        if(args.length == 0 || args.length > 2){
            player.sendMessage("Invalid Syntax!");
            return true;
        }

        switch (args[0].toLowerCase()){
            case "start" -> {
                if(args.length == 1) {
                    Optional<ArenaTemplate> template = Game.getArenaTemplateRegistry().getRandomArena();
                    startRound(player, template.map(ArenaTemplate::id).orElse(null));
                }
                else startRound(player, args[1]);
            }
            case "stop" -> stopRound(player);
            case "reload" -> reload(sender);
            case "state" -> {
                if(args.length == 1) getState(player);
                else switchState(player, args[1]);
            }
            case "debug" -> {
                if(args.length == 1) sendDebugMessage(player);
                else changeDebugViewer(player, args[1]);
            }
            default -> player.sendMessage("Invalid Syntax!");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(args.length == 1){
            return Stream.of("start", "stop", "state", "debug", "reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        } else if(args.length == 2){
            if(args[0].equalsIgnoreCase("state")){
                return RoundState
                        .getListOfStates()
                        .stream()
                        .filter(s -> s.startsWith(args[1].toUpperCase()))
                        .toList();
            } else if (args[0].equalsIgnoreCase("debug")){
                return Stream.of("on", "off")
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            } else if(args[0].equalsIgnoreCase("start")){
                return Game.getArenaTemplateRegistry().getValidArenas().keySet().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }

        return List.of();
    }


    // Subcommands
    private void startRound(Player owner, @Nullable String arena){
        RoundManager roundManager = Game.getRoundManager();
        ArenaTemplateRegistry templateRegistry = Game.getArenaTemplateRegistry();

        if(roundManager.hasRound(owner.getUniqueId())){
            owner.sendMessage("Active Round already open!");
            return;
        }

        if(arena == null || !templateRegistry.getValidArenas().containsKey(arena)){
            owner.sendMessage("Arena not valid!");
            return;
        }

        owner.sendMessage("Registering new Round in arena '" + arena + "' for '" + owner.getName() + "'.");
        roundManager.registerRound(owner, arena);
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
        owner.sendMessage("Trying to switch State from '" + round.getState().name() + "' to '" + stateString + "' for Round for '" + owner.getName() + "'.");
        round.transitionTo(state);
    }

    private void sendDebugMessage(Player owner){
        Round round = Game.getRoundManager().getRound(owner.getUniqueId());
        if(round == null){
            owner.sendMessage("No active Round!");
            return;
        }

        owner.sendMessage("Current Round from '" + owner.getName() + "':");
        owner.sendMessage(round.getDebugMessage());
    }

    private void changeDebugViewer(Player owner, String argument){
        RoundManager manager = Game.getRoundManager();

        if(argument.equalsIgnoreCase("on")){
            if(!manager.addDebugViewer(owner.getUniqueId())) {
                owner.sendMessage("Already Debug Viewer!");
                return;
            }

            owner.sendMessage("Added to Debug Viewers");
        } else if(argument.equalsIgnoreCase("off")){
            if(!manager.removeDebugViewer(owner.getUniqueId())){
                owner.sendMessage("No Debug Viewer!");
                return;
            }

            owner.sendMessage("Removed from Debug Viewers");
        } else {
            owner.sendMessage("Invalid Argument!");
        }
    }

    private void reload(CommandSender sender){
        ArenaTemplateRegistry arenaTemplateRegistry = Game.getArenaTemplateRegistry();
        TemplateScanResult result = arenaTemplateRegistry.reload();
        sender.sendMessage(Component.text(
                "Reloaded arenas: " + result.valid().size() + " valid, " + result.errors().size() + " broken.",
                result.hasErrors() ? NamedTextColor.YELLOW : NamedTextColor.GREEN
        ));
    }
}
