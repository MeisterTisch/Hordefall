package user.meistertisch.de.game;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.util.FileUtils;
import user.meistertisch.de.game.round.CommandRound;
import user.meistertisch.de.game.round.EventJoin;
import user.meistertisch.de.game.round.EventVanillaCancel;
import user.meistertisch.de.game.round.RoundManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

public final class Game extends JavaPlugin {
    private static Plugin plugin;
    private static RoundManager roundManager;

    @Override
    public void onEnable(){
        PluginManager pluginManager = Bukkit.getPluginManager();
        plugin = this;
        saveDefaultConfig();

        cleanupOrphanedWorlds();

        roundManager = new RoundManager();
        roundManager.startTickTask();

        // EVENTS
        pluginManager.registerEvents(new EventJoin(), this);
        pluginManager.registerEvents(new EventVanillaCancel(), this);

        // COMMANDS
        getCommand("round").setExecutor(new CommandRound());
    }

    @Override
    public void onDisable() {
        roundManager.stopTickTask();
    }

    public static Plugin getPlugin(){
        return plugin;
    }

    public static RoundManager getRoundManager() {
        return roundManager;
    }

    private void cleanupOrphanedWorlds() {
        Path dimensionsDir = getServer().getLevelDirectory().resolve("dimensions/minecraft");
        File[] leftoverWorlds = dimensionsDir.toFile().listFiles((dir, name) -> name.startsWith("round_"));
        if (leftoverWorlds != null) {
            for (File worldDir : leftoverWorlds) {
                try {
                    FileUtils.deleteDirectory(worldDir);
                    getLogger().info("Cleaned up orphaned round world: " + worldDir.getName());
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, "Failed to clean up leftover world: " + worldDir.getName(), e);
                }
            }
        }
    }
}
