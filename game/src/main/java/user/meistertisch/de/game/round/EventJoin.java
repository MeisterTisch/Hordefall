package user.meistertisch.de.game.round;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
            Game.getRoundManager().registerRound(event.getPlayer()); // TODO: Remove or refactor. It's for debug currently
            event.getPlayer().kick(Component.text("No active round!"), PlayerKickEvent.Cause.ILLEGAL_ACTION);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){

    }
}
