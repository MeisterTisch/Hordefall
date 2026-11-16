package user.meistertisch.de.game.round;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.util.FileUtils;
import user.meistertisch.de.game.Game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public class Round {
    private UUID owner;
    private RoundState state = RoundState.PREPARING;
    private long tickCounter = 0;
    private World arenaWorld;
    private double tempMovementSpeed = 0;

    public Round(UUID owner) {
        this.owner = owner;
    }

    void tick() {
        // Only advance the run timer while actively running (freeze on LEVEL_UP or PAUSED)
        if (state != RoundState.RUNNING) {
//            return; // TODO: uncomment return. Only for debug currently
        }

        if (state == RoundState.WAITING) {
            return; // TODO: remove return. Only for debug currently
        }

        if(state == RoundState.COUNTDOWN && tickCounter % 20 == 0){
            Player player = Bukkit.getPlayer(owner);
            assert player != null;
            player.sendMessage(Component.text("Countdown: " + tickCounter % 20));
        }

        if(tickCounter > 45 * 20){
            transitionTo(RoundState.RUNNING);
        } else if(tickCounter > 15 * 20){
            transitionTo(RoundState.LEVEL_UP);
        } else if(tickCounter > 5 * 20){
            transitionTo(RoundState.RUNNING);
        }

        tickCounter++;

        if(tickCounter % (5 * 20) == 0){
            System.out.println("Tick, Status: " + state);
        }

        // Optional: check if the 30-minute run limit (36,000 ticks) is reached[cite: 5, 34]
        if (tickCounter >= 1 * 60 * 20) { // minutes * 60 seconds * 20 ticks/second
            transitionTo(RoundState.ENDING);
        }
    }

    // States
    void transitionTo(RoundState next) {
        if (this.state == next || !isValidTransition(this.state, next)) {
            return; // Ignore duplicate or illegal transitions
        }

        RoundState previous = this.state;
        this.state = next;



        // 1. Handle leaving the previous state
        if (previous == RoundState.LEVEL_UP) {
            unfreezePlayer(); // Restore movement & jump attributes
        }

        // 2. Handle entering the new state
        switch (next) {
            // TODO
            case COUNTDOWN -> preparePlayer();
            case LEVEL_UP -> freezePlayer(); // Set speed & jump strength to 0
            case ENDING -> cleanup();        // Cancel task, clear monsters/projectile
            default -> {}
        }
    }

    private boolean isValidTransition(RoundState from, RoundState to) {
        return switch (from) {
            case PREPARING -> to == RoundState.WAITING;
            case WAITING   -> to == RoundState.COUNTDOWN || to == RoundState.RUNNING || to == RoundState.ENDING;
            case COUNTDOWN, LEVEL_UP, PAUSED -> to == RoundState.RUNNING || to == RoundState.ENDING;
            case RUNNING   -> to == RoundState.LEVEL_UP || to == RoundState.PAUSED || to == RoundState.ENDING;
            case ENDING    -> false; // Terminal state: once ending, never go back
        };
    }


    // Round and Server
    public CompletableFuture<Void> prepare(Plugin plugin, String arenaName) {
        String worldName = "round_" + owner;
        Path levelPath = Game.getPlugin().getServer().getLevelDirectory();
        File templateDir = levelPath.resolve("dimensions/minecraft/templates/" + arenaName).toFile();
        File overworldSubdir = new File(templateDir, "dimensions/minecraft/overworld");
        File sourceDir = overworldSubdir.exists() ? overworldSubdir : templateDir;
        File targetDir = levelPath.resolve("dimensions/minecraft").resolve(worldName).toFile();

        // 1. Copy template files asynchronously (off the main thread)
        return CompletableFuture.runAsync(() -> {
            try {
                FileUtils.copyDirectoryStructure(sourceDir, targetDir);
                // Delete uid.dat and session.lock so Bukkit treats it as a fresh world
                Files.deleteIfExists(targetDir.toPath().resolve("uid.dat"));
                Files.deleteIfExists(targetDir.toPath().resolve("session.lock"));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }).thenCompose(v -> {
            // 2. Load the world on the main thread
            CompletableFuture<Void> mainThreadFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    World world = new WorldCreator(worldName).createWorld();
                    if (world == null) {
                        throw new IllegalStateException("Failed to load arena world: " + worldName);
                    }

                    // Disable saving and vanilla mechanics
                    world.setAutoSave(false);
                    world.setGameRule(GameRules.SPAWN_MOBS, false);
                    world.setGameRule(GameRules.ADVANCE_TIME, false);
                    world.setGameRule(GameRules.ADVANCE_WEATHER, false);
                    world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);

                    this.arenaWorld = world;
                    transitionTo(RoundState.WAITING); // Ready for player!
                    mainThreadFuture.complete(null);
                } catch (Exception e) {
                    mainThreadFuture.completeExceptionally(e);
                }
            });
            return mainThreadFuture;
        });
    }

    private void preparePlayer() {
        Player player;
        do {
            player = Bukkit.getPlayer(owner);
        } while (player == null);

        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) attr.setBaseValue(0.1);
        attr = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (attr != null) attr.setBaseValue(0);
    }

    private void freezePlayer(){
        Player player;
        do {
            player = Bukkit.getPlayer(owner);
        } while(player == null);

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        tempMovementSpeed = (speedAttr != null) ? speedAttr.getBaseValue() : 0.1;

        if (speedAttr != null) {
            speedAttr.setBaseValue(0.0);
        }
    }

    private void unfreezePlayer(){
        Player player = Bukkit.getPlayer(owner);
        if(player == null){
            return;
        }

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(tempMovementSpeed);
        }
    }

    private void cleanup() {
        // 1. Restore player state (speed, health scale, clear inventory/effects)
        // 2. Remove all spawned monsters, projectiles, and display entities (Milestone 2+)[cite: 37]
        // 3. Clear internal maps/lists so nothing leaks[cite: 37]
        kickPlayers();
        cleanupWorld();
    }

    private void cleanupWorld() {
        if (arenaWorld == null) return;

        if(!arenaWorld.getPlayers().isEmpty()){
            arenaWorld.getPlayers().forEach(Player::kick);
        }

        File worldFolder = arenaWorld.getWorldFolder();

        // 1. Unload without saving on the main thread[cite: 19, 36]
        Bukkit.unloadWorld(arenaWorld, false);
        this.arenaWorld = null;

        // 2. Delete the folder asynchronously so disk I/O doesn't stall the server[cite: 37]
        CompletableFuture.runAsync(() -> {
            try {
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException e) {
                Game.getPlugin().getLogger().log(Level.SEVERE, "Couldn't delete the folder!", e);
            }
        });
    }

    private void kickPlayers(){
        if (arenaWorld == null) return;

        if(!arenaWorld.getPlayers().isEmpty()){
            arenaWorld.getPlayers().forEach(player -> player.kick(Component.text("Thanks for playing. Round ended.")));
        }
    }

    // GETTER
    public RoundState getState() {
        return state;
    }

    public UUID getOwner() {
        return owner;
    }

    public long getTickCounter() {
        return tickCounter;
    }

    public int getElapsedSeconds() {
        return (int) (tickCounter / 20L);
    }


    // SETTER
    public void setState(RoundState state) {
        this.state = state;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public World getWorld() {
        return arenaWorld;
    }
}
