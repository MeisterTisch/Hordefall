package user.meistertisch.de.game;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import user.meistertisch.de.game.round.RoundManager;

public final class Game extends JavaPlugin {
    private static Plugin plugin;
    private RoundManager roundManager;

    @Override
    public void onEnable(){
        plugin = this;
        saveDefaultConfig();

        roundManager = new RoundManager();
        roundManager.startTickTask();
    }

    @Override
    public void onDisable() {
        roundManager.stopTickTask();
    }

    public static Plugin getPlugin(){
        return plugin;
    }
}
