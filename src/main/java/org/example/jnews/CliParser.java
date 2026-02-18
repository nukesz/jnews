package org.example.jnews;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CliParser {
    public CliConfig parse(String[] args) {
        Mode mode = Mode.TODAY;
        List<String> topics = new ArrayList<>();
        boolean saveTopics = false;
        boolean clearSavedTopics = false;
        boolean help = false;
        Integer showIndex = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--today" -> mode = Mode.TODAY;
                case "--week" -> mode = Mode.WEEK;
                case "--month" -> mode = Mode.MONTH;
                case "--since-last" -> mode = Mode.SINCE_LAST;
                case "--save-topics" -> saveTopics = true;
                case "--clear-saved-topics" -> clearSavedTopics = true;
                case "--help", "-h" -> help = true;
                case "--show" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--show requires an index value");
                    }
                    String raw = args[++i].trim();
                    try {
                        int idx = Integer.parseInt(raw);
                        if (idx < 1) {
                            throw new IllegalArgumentException("--show index must be >= 1");
                        }
                        showIndex = idx;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("--show requires a numeric index (example: --show 3)");
                    }
                }
                case "--topic" -> {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--topic requires a value");
                    }
                    topics.add(args[++i].trim().toLowerCase(Locale.ROOT));
                }
                default -> throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return new CliConfig(mode, List.copyOf(topics), saveTopics, clearSavedTopics, help, showIndex);
    }

    public String helpText() {
        return """
                Usage: ./jnews [--today|--week|--month|--since-last] [--topic <name>]...
                               [--show <index>] [--save-topics] [--clear-saved-topics] [--help]

                Examples:
                  ./jnews --today
                  ./jnews --week --topic ai --topic politics
                  ./jnews --week --show 2
                  ./jnews --month --topic business --save-topics
                  ./jnews --since-last
                """;
    }
}
