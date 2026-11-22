package user.meistertisch.de.game.round.arena;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class ArenaTemplateValidator {
    private static final String LEVEL_DAT = "level.dat";
    private static final String REGION_DIR = "region";
    private static final String SPAWN_FILE = "spawn.yml";

    public TemplateScanResult scan(Path templatesDir) {
        Map<String, ArenaTemplate> valid = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();

        if (templatesDir == null || !Files.isDirectory(templatesDir)) {
            errors.put("templates", "templates directory does not exist");
            return new TemplateScanResult(valid, errors);
        }

        List<Path> candidates;
        try (Stream<Path> entries = Files.list(templatesDir)) {
            candidates = entries
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            errors.put("templates", "failed to read templates directory: " + e.getMessage());
            return new TemplateScanResult(valid, errors);
        }

        if (candidates.isEmpty()) {
            errors.put("templates", "no arena template folders found");
            return new TemplateScanResult(valid, errors);
        }

        // Create a fresh Yaml instance per scan; SnakeYAML's Yaml class is not thread-safe
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

        for (Path arenaDir : candidates) {
            String id = arenaDir.getFileName().toString();
            validateArena(id, arenaDir, yaml, valid, errors);
        }

        return new TemplateScanResult(valid, errors);
    }

    private void validateArena(
            String id,
            Path arenaDir,
            Yaml yaml,
            Map<String, ArenaTemplate> valid,
            Map<String, String> errors
    ) {
        if (!Files.isRegularFile(arenaDir.resolve(LEVEL_DAT))) {
            errors.put(id, "missing level.dat");
            return;
        }
        if (!Files.isDirectory(arenaDir.resolve(REGION_DIR))) {
            errors.put(id, "missing region directory");
            return;
        }

        Path spawnPath = arenaDir.resolve(SPAWN_FILE);
        if (!Files.isRegularFile(spawnPath)) {
            errors.put(id, "missing spawn.yml");
            return;
        }

        Object loaded;
        try (Reader reader = Files.newBufferedReader(spawnPath, StandardCharsets.UTF_8)) {
            loaded = yaml.load(reader);
        } catch (YAMLException e) {
            errors.put(id, "malformed spawn.yml: " + e.getMessage());
            return;
        } catch (IOException e) {
            errors.put(id, "failed to read spawn.yml: " + e.getMessage());
            return;
        }

        if (!(loaded instanceof Map<?, ?> rawMap)) {
            errors.put(id, "spawn.yml must contain a key-value mapping");
            return;
        }

        try {
            double x = readRequiredDouble(rawMap, "x");
            double y = readRequiredDouble(rawMap, "y");
            double z = readRequiredDouble(rawMap, "z");
            float yaw = readOptionalFloat(rawMap, "yaw", 0.0f);
            float pitch = readOptionalFloat(rawMap, "pitch", 0.0f);

            ArenaSpawn spawn = new ArenaSpawn(x, y, z, yaw, pitch);
            valid.put(id, new ArenaTemplate(id, arenaDir, spawn));
        } catch (IllegalArgumentException e) {
            errors.put(id, e.getMessage());
        }
    }

    private double readRequiredDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("spawn.yml missing required '" + key + "' coordinate");
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("spawn.yml '" + key + "' must be a number");
        }
        double result = number.doubleValue();
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("spawn.yml '" + key + "' must be a finite number");
        }
        return result;
    }

    private float readOptionalFloat(Map<?, ?> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("spawn.yml '" + key + "' must be a number");
        }
        float result = number.floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException("spawn.yml '" + key + "' must be a finite number");
        }
        return result;
    }
}
