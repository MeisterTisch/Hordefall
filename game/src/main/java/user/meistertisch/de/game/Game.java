package user.meistertisch.de.game;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import user.meistertisch.de.game.round.EventJoin;
import user.meistertisch.de.game.round.EventVanillaCancel;
import user.meistertisch.de.game.round.RoundManager;

public final class Game extends JavaPlugin {
    private static Plugin plugin;
    private static RoundManager roundManager;

    @Override
    public void onEnable(){
        PluginManager pluginManager = Bukkit.getPluginManager();
        plugin = this;
        saveDefaultConfig();

        roundManager = new RoundManager();
        roundManager.startTickTask();

        // EVENTS
        pluginManager.registerEvents(new EventJoin(), this);
        pluginManager.registerEvents(new EventVanillaCancel(), this);
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
}
