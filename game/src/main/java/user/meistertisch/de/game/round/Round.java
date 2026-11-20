package user.meistertisch.de.game.round;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.codehaus.plexus.util.FileUtils;
import user.meistertisch.de.game.Game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Round {
    private static final int ROUND_DURATION = 30 * 60 * 20; // minutes * 60 seconds * 20 ticks/second
    private UUID owner;
    private RoundState state = RoundState.PREPARING;
    private long runningTickCounter = 0;
    private long tickCounter = 0;
    private World arenaWorld;
    private double tempMovementSpeed = 0;

    private int mobsQuantity = 0;

    // Debug
    private final Set<UUID> currentDebugViewers = new HashSet<>();
    private static final int SAMPLE_SIZE = 20; // Rolling average over the last 20 ticks (1 second)
    private final long[] tickSamplesNanos = new long[SAMPLE_SIZE];
    private int sampleIndex = 0;
    private int sampleCount = 0;
    private BossBar debugBar;

    public Round(UUID owner) {
        this.owner = owner;
    }

    void tick() {
        tickCounter++;

        // Only advance the run timer while actively running (freeze on LEVEL_UP or PAUSED)
        if (state == RoundState.RUNNING) {
            long startNanos = System.nanoTime();

            runningTickCounter++;
            // HERE LOGIC FOR TICKING (Monsters, Projectiles, ...)

            long durationNanos = System.nanoTime() - startNanos;
            recordTickDuration(durationNanos);
        }

        if (tickCounter % 20 == 0) {
            updateBossBar();
            sendTimeActionBar();
        }

        // Optional: check if the 30-minute run limit (36,000 ticks) is reached
        if (runningTickCounter >= ROUND_DURATION) {
            transitionTo(RoundState.ENDING);
        }
    }

    // Debug
    private void recordTickDuration(long durationNanos) {
        tickSamplesNanos[sampleIndex] = durationNanos;
        sampleIndex = (sampleIndex + 1) % SAMPLE_SIZE;
        if (sampleCount < SAMPLE_SIZE) {
            sampleCount++;
        }
    }

    public double getAverageTickMillis() {
        if (sampleCount == 0) return 0.0;
        long sum = 0;
        for (int i = 0; i < sampleCount; i++) {
            sum += tickSamplesNanos[i];
        }
        // Divide by sampleCount, then divide by 1,000,000 to convert nanoseconds -> milliseconds
        return (sum / (double) sampleCount) / 1_000_000.0;
    }

    public double getMaxTickMillis() {
        long max = 0;
        for (int i = 0; i < sampleCount; i++) {
            if (tickSamplesNanos[i] > max) {
                max = tickSamplesNanos[i];
            }
        }
        return max / 1_000_000.0;
    }

    private void updateBossBar() {
        // 1. Determine who SHOULD see the boss bar right now
        // (Active debug viewers who are online AND inside this round's arena world)
        Set<UUID> desiredViewers = Game.getRoundManager().getDebugViewers().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> player.getWorld().equals(arenaWorld))
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());

        // 2. Identify new viewers: in desired, but not yet viewing
        for (UUID uuid : desiredViewers) {
            if (!currentDebugViewers.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.showBossBar(debugBar);
                }
            }
        }

        // 3. Identify removed viewers: viewing, but no longer desired
        // (Ran /round debug off, teleported to lobby/another world, or logged out)
        Iterator<UUID> iterator = currentDebugViewers.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            if (!desiredViewers.contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.hideBossBar(debugBar);
                }
                iterator.remove();
            }
        }

        // 4. Update the tracker
        currentDebugViewers.addAll(desiredViewers);

        // 5. Update the BossBar
        //State: RUNNING | Time: 04:12 | Tick: 0.42 ms (max 1.15 ms) | Mobs: 284
        float progress = (float) runningTickCounter / ROUND_DURATION;
        Component title = getDebugMessage();

        if(debugBar == null)
            debugBar = BossBar.bossBar(title, progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        else {
            debugBar.name(title);
            debugBar.progress(progress);
        }
    }

    private void hideBossBar(){
        arenaWorld.getPlayers().forEach(player -> player.hideBossBar(debugBar));
    }

    public Component getDebugMessage() {
        Duration duration = Duration.ofMillis(runningTickCounter * 50L);
        String time = String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
        double tickAverage = getAverageTickMillis();
        double tickMax = getMaxTickMillis();

        return MiniMessage.miniMessage().deserialize(
                "State: <state> | Time: <time> | Tick: <avg> ms (Max: <max> ms) | Mobs: <mobs>",
                Placeholder.unparsed("state", state.name()),
                Placeholder.unparsed("time", time),
                Placeholder.unparsed("avg", String.format("%.2f", tickAverage)),
                Placeholder.unparsed("max", String.format("%.2f", tickMax)),
                Placeholder.unparsed("mobs", String.valueOf(mobsQuantity))
        );
    }

    // States
    void transitionTo(RoundState next) {
        if (this.state == next || !isValidTransition(this.state, next)) {
            return; // Ignore duplicate or illegal transitions
        }

        RoundState previous = this.state;
        this.state = next;

        // 1. Handle leaving the previous state
        if (previous == RoundState.LEVEL_UP || previous == RoundState.PAUSED) {
            unfreezePlayer(); // Restore movement & jump attributes
        }

        // 2. Handle entering the new state
        switch (next) {
            // TODO
            case COUNTDOWN -> preparePlayer();
            case LEVEL_UP, PAUSED -> freezePlayer(); // Set speed & jump strength to 0
            case ENDING -> cleanup();        // Cancel task, clear monsters/projectile
            default -> {
            }
        }
    }

    private boolean isValidTransition(RoundState from, RoundState to) {
        return switch (from) {
            case PREPARING -> to == RoundState.WAITING;
            case WAITING -> to == RoundState.COUNTDOWN || to == RoundState.RUNNING || to == RoundState.ENDING;
            case COUNTDOWN, LEVEL_UP, PAUSED -> to == RoundState.RUNNING || to == RoundState.ENDING;
            case RUNNING -> to == RoundState.LEVEL_UP || to == RoundState.PAUSED || to == RoundState.ENDING;
            case ENDING -> false; // Terminal state: once ending, never go back
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

    private void freezePlayer() {
        Player player;
        do {
            player = Bukkit.getPlayer(owner);
        } while (player == null);

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        tempMovementSpeed = (speedAttr != null) ? speedAttr.getBaseValue() : 0.1;

        if (speedAttr != null) {
            speedAttr.setBaseValue(0.0);
        }
    }

    private void unfreezePlayer() {
        Player player = Bukkit.getPlayer(owner);
        if (player == null) {
            return;
        }

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(tempMovementSpeed);
        }
    }

    private void sendTimeActionBar(){
        Duration duration = Duration.ofMillis(runningTickCounter * 50L);
        String timeStr = String.format("Playtime: %02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
        Component time = Component.text(timeStr);

        arenaWorld.getPlayers().forEach(player -> player.sendActionBar(time));
    }

    // Cleanup
    private void cleanup() {
        // 1. Restore player state (speed, health scale, clear inventory/effects)
        // 2. Remove all spawned monsters, projectiles, and display entities (Milestone 2+)[cite: 37]
        // 3. Clear internal maps/lists so nothing leaks[cite: 37]
        hideBossBar();
        kickPlayers();
        cleanupWorld();
    }

    private void cleanupWorld() {
        if (arenaWorld == null) return;

        // 1. Move all players out immediately so the world has 0 players
        World fallbackWorld = Bukkit.getWorlds().getFirst(); // Root overworld
        for (Player player : arenaWorld.getPlayers()) {
            if (fallbackWorld != null && !fallbackWorld.equals(arenaWorld)) {
                player.teleport(fallbackWorld.getSpawnLocation());
            }
            player.kick(Component.text("Thanks for playing. Round ended."));
        }

        // 2. Unload the world and ALWAYS verify the return boolean
        File worldFolder = arenaWorld.getWorldFolder();
        boolean unloaded = Bukkit.unloadWorld(arenaWorld, false);
        this.arenaWorld = null;

        if (!unloaded) {
            Game.getPlugin().getLogger().warning("Failed to unload arena world: " + worldFolder.getName());
            return;
        }

        // 3. Delete the folder once we know it is 100% unloaded
        deleteWorldFolder(worldFolder, false);
    }

    public void deleteWorldFolder(File worldFolder, boolean isShutdown) {
        Runnable deleteAction = () -> {
            try {
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException e) {
                Game.getPlugin().getLogger().log(Level.SEVERE, "Couldn't delete arena folder: " + worldFolder.getName(), e);
            }
        };

        if (isShutdown) {
            // Run synchronously during shutdown so JVM doesn't terminate mid-deletion
            deleteAction.run();
        } else {
            CompletableFuture.runAsync(deleteAction);
        }
    }

    private void kickPlayers() {
        if (arenaWorld == null) return;

        if (!arenaWorld.getPlayers().isEmpty()) {
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

    public long getRunningTickCounter() {
        return runningTickCounter;
    }

    public int getElapsedSeconds() {
        return (int) (runningTickCounter / 20L);
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
