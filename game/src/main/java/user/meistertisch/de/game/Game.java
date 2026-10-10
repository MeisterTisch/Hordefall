package user.meistertisch.de.game;

import org.bukkit.plugin.java.JavaPlugin;

public final class Game extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
