package org.example.jnews;

import java.util.List;

public record CliConfig(
        Mode mode,
        List<String> topics,
        boolean saveTopics,
        boolean clearSavedTopics,
        boolean help,
        Integer showIndex
) {
    public static CliConfig defaults() {
        return new CliConfig(Mode.TODAY, List.of(), false, false, false, null);
    }
}
