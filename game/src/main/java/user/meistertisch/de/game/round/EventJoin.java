package user.meistertisch.de.game.round;

import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.kyori.adventure.text.Component;
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
    public void onJoin(PlayerJoinEvent event){
        UUID uuid = event.getPlayer().getUniqueId();
        Round round = Game.getRoundManager().getRound(uuid);

        if(round == null){
//            event.getPlayer().kick(Component.text("No active round!"), PlayerKickEvent.Cause.ILLEGAL_ACTION); // TODO: Uncommment kick, removed due to debug
        } else {
            round.transitionTo(RoundState.COUNTDOWN);
            event.getPlayer().teleport(new Location(round.getWorld(), 7.5, 0.2, 22.5, 180, 0));
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
