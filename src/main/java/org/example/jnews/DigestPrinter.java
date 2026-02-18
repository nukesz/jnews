package org.example.jnews;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DigestPrinter {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
    private final ArticleReader reader;

    public DigestPrinter(ArticleReader reader) {
        this.reader = reader;
    }

    public void printDigest(List<Article> articles, TimeWindow window, Set<String> requestedTopics, Mode mode) {
        ZoneId zone = ZoneId.systemDefault();
        String modeLabel = switch (mode) {
            case TODAY -> "today";
            case WEEK -> "week";
            case MONTH -> "month";
            case SINCE_LAST -> "since-last";
        };
        System.out.printf("jnews digest (%s)%n", modeLabel);
        System.out.printf("Window: %s -> %s%n",
                DISPLAY_TIME.format(ZonedDateTime.ofInstant(window.from(), zone)),
                DISPLAY_TIME.format(ZonedDateTime.ofInstant(window.to(), zone)));
        if (!requestedTopics.isEmpty()) {
            System.out.printf("Topics: %s%n", String.join(", ", requestedTopics));
        }
        if (articles.isEmpty()) {
            System.out.println();
            System.out.println("No matching stories found in this window.");
            return;
        }

        System.out.printf("Stories: %d%n%n", articles.size());
        System.out.println("Top headlines:");
        int max = Math.min(12, articles.size());
        for (int i = 0; i < max; i++) {
            Article a = articles.get(i);
            System.out.printf("%d. [%s|%s] %s (%s)%n   %s%n",
                    i + 1,
                    a.topic(),
                    a.source(),
                    a.title(),
                    DISPLAY_TIME.format(ZonedDateTime.ofInstant(a.published(), zone)),
                    a.link());
        }
        System.out.println();
        System.out.println("Read one in terminal with: ./jnews --show <index> [same filters]");

        Map<String, List<Article>> byTopic = articles.stream().collect(Collectors.groupingBy(Article::topic));
        System.out.println();
        System.out.println("Topic snapshot:");
        byTopic.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .forEach(entry -> {
                    List<Article> newest = entry.getValue().stream().limit(2).toList();
                    String highlights = newest.stream().map(Article::title).collect(Collectors.joining(" | "));
                    System.out.printf("- %s: %d story(ies). %s%n", entry.getKey(), entry.getValue().size(), highlights);
                });
    }

    public void printArticle(List<Article> articles, int index) {
        if (articles.isEmpty()) {
            System.out.println("No stories found for this filter window.");
            return;
        }
        if (index < 1 || index > articles.size()) {
            System.err.printf("Invalid --show index %d. Available range: 1..%d%n", index, articles.size());
            return;
        }

        Article article = articles.get(index - 1);
        ZoneId zone = ZoneId.systemDefault();
        System.out.printf("[%s|%s] %s%n", article.topic(), article.source(), article.title());
        System.out.printf("Published: %s%n", DISPLAY_TIME.format(ZonedDateTime.ofInstant(article.published(), zone)));
        System.out.printf("Link: %s%n%n", article.link());

        String content = reader.readText(article.link());
        if (!content.isBlank()) {
            System.out.println(content);
            return;
        }

        System.out.println("Could not extract full body text from this page.");
        if (!article.description().isBlank()) {
            System.out.println();
            System.out.println("Feed summary:");
            System.out.println(article.description());
        }
    }
}
