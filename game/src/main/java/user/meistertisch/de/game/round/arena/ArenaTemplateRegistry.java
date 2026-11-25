package user.meistertisch.de.game.round.arena;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;

public final class ArenaTemplateRegistry {
    private final Path templatesDir;
    private final ArenaTemplateValidator validator;
    private final Logger logger;

    private volatile TemplateScanResult currentResult = new TemplateScanResult(Map.of(), Map.of());

    public ArenaTemplateRegistry(Path templatesDir, ArenaTemplateValidator validator, Logger logger) {
        this.templatesDir = Objects.requireNonNull(templatesDir, "templatesDir must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    public TemplateScanResult reload() {
        TemplateScanResult result = validator.scan(templatesDir);
        this.currentResult = result;

        logger.info("Loaded {} valid arena template(s) from '{}'.",
                result.valid().size(), templatesDir.toAbsolutePath().normalize());

        result.errors().forEach((id, reason) ->
                logger.error("Arena template '{}' failed validation: {}", id, reason));

        if (!result.hasUsableArenas()) {
            logger.error("============================================================");
            logger.error("CRITICAL: No valid arena templates are available!");
            logger.error("Rounds cannot start until at least one valid template exists in '{}'.",
                    templatesDir.toAbsolutePath().normalize());
            logger.error("============================================================");
        }

        return result;
    }

    public Optional<ArenaTemplate> getArena(String id) {
        return currentResult.find(id);
    }

    public Optional<ArenaTemplate> getRandomArena() {
        return currentResult.pickRandom();
    }

    public Map<String, ArenaTemplate> getValidArenas() {
        return currentResult.valid();
    }

    public Map<String, String> getBrokenArenas() {
        return currentResult.errors();
    }

    public boolean hasUsableArenas() {
        return currentResult.hasUsableArenas();
    }

    public boolean hasErrors() {
        return currentResult.hasErrors();
    }

    public boolean isUsableArena(ArenaTemplate arena) {
        return getValidArenas().containsValue(arena);
    }
}
