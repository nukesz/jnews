package org.example.jnews;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class JNewsApplication {
    private final CliParser cliParser = new CliParser();
    private final StateStore stateStore = new StateStore(Path.of(System.getProperty("user.home"), ".jnews.properties"));
    private final TopicClassifier topicClassifier = new TopicClassifier();
    private final NewsService newsService = new NewsService(topicClassifier);
    private final DigestPrinter digestPrinter = new DigestPrinter(new ArticleReader());

    public void run(String[] args) {
        CliConfig config;
        try {
            config = cliParser.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.out.println(cliParser.helpText());
            return;
        }

        if (config.help()) {
            System.out.println(cliParser.helpText());
            return;
        }

        AppState state = stateStore.load();
        Set<String> requestedTopics = resolveRequestedTopics(config, state);
        Instant now = Instant.now();
        TimeWindow window = resolveWindow(config, state, now);

        var articles = newsService.fetchArticles(window, requestedTopics);
        if (config.showIndex() != null) {
            digestPrinter.printArticle(articles, config.showIndex());
        } else {
            digestPrinter.printDigest(articles, window, requestedTopics, config.mode());
        }

        stateStore.save(now, config, state);
    }

    private Set<String> resolveRequestedTopics(CliConfig config, AppState state) {
        if (!config.topics().isEmpty()) {
            return Set.copyOf(config.topics());
        }
        return new HashSet<>(state.savedTopics());
    }

    private TimeWindow resolveWindow(CliConfig config, AppState state, Instant now) {
        Instant from = switch (config.mode()) {
            case TODAY -> now.minus(Duration.ofDays(1));
            case WEEK -> now.minus(Duration.ofDays(7));
            case MONTH -> now.minus(Duration.ofDays(30));
            case SINCE_LAST -> state.lastRun().orElse(now.minus(Duration.ofDays(7)));
        };
        return new TimeWindow(from, now);
    }
}
