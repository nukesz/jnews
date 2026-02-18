package org.example.jnews;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public record AppState(Optional<Instant> lastRun, Set<String> savedTopics) {
    public static AppState empty() {
        return new AppState(Optional.empty(), Set.of());
    }
}
