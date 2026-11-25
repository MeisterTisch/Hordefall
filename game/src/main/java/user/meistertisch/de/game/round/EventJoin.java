package user.meistertisch.de.game.round;

import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.codehaus.plexus.util.FileUtils;
import user.meistertisch.de.game.Game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

public class EventJoin implements Listener {
    @EventHandler
    public void onJoinForRound(PlayerJoinEvent event){
        UUID uuid = event.getPlayer().getUniqueId();
        Round round = Game.getRoundManager().getRound(uuid);

        if(round == null){
//            event.getPlayer().kick(Component.text("No active round!"), PlayerKickEvent.Cause.ILLEGAL_ACTION); // TODO: Uncommment kick, removed due to debug
        }
    }

    @EventHandler
    public void onJoinForNoValidArenas(PlayerJoinEvent event){
        if(!Game.getArenaTemplateRegistry().hasUsableArenas()){
            Component message = Component.text()
                    .append(Component.text("Warning: No valid arena templates are loaded! New rounds are currently blocked. ", NamedTextColor.RED))
                    .append(Component.text("\n[Click here to get more details]", NamedTextColor.YELLOW)
                            .clickEvent(ClickEvent.runCommand("/round arenas"))
                            .hoverEvent(HoverEvent.showText(Component.text("Click!", NamedTextColor.GRAY))))
                    .build();

            event.getPlayer().sendMessage(message);
        }

    }

    @EventHandler
    public void onWorldChange(PlayerClientLoadedWorldEvent event){
        Player player = event.getPlayer();

        Round round = Game.getRoundManager().getRound(player.getUniqueId());
        if(round == null){
            return;
        }

        if(round.getWorld() == player.getWorld() && !event.isTimeout()){
            round.transitionTo(RoundState.COUNTDOWN);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){

    }
}
