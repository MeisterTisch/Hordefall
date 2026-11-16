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
            case "stop" -> player.sendMessage("Stop todo");
            case "state" -> player.sendMessage("State todo");
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
        Game.getRoundManager().registerRound(owner, arena);
    }
}
