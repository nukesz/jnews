package org.example.jnews;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class StateStore {
    private final Path file;

    public StateStore(Path file) {
        this.file = file;
    }

    public AppState load() {
        if (!Files.exists(file)) {
            return AppState.empty();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("Failed to read " + file + ": " + e.getMessage());
            return AppState.empty();
        }

        Optional<Instant> lastRun = Optional.ofNullable(props.getProperty("last_run"))
                .flatMap(this::tryParseInstant);
        Set<String> savedTopics = parseTopics(props.getProperty("saved_topics", ""));
        return new AppState(lastRun, savedTopics);
    }

    public void save(Instant now, CliConfig config, AppState previous) {
        Properties props = new Properties();
        props.setProperty("last_run", now.toString());

        Set<String> topicsToSave = previous.savedTopics();
        if (config.clearSavedTopics()) {
            topicsToSave = Set.of();
        }
        if (config.saveTopics()) {
            if (config.topics().isEmpty()) {
                System.err.println("No --topic values provided; skipping topic save.");
            } else {
                topicsToSave = Set.copyOf(config.topics());
            }
        }
        if (!topicsToSave.isEmpty()) {
            props.setProperty("saved_topics", String.join(",", topicsToSave));
        }

        try (var out = Files.newOutputStream(file)) {
            props.store(out, "jnews state");
        } catch (IOException e) {
            System.err.println("Failed to write " + file + ": " + e.getMessage());
        }
    }

    private Optional<Instant> tryParseInstant(String raw) {
        try {
            return Optional.of(Instant.parse(raw));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private Set<String> parseTopics(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
