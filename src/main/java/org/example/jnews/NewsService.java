package org.example.jnews;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class NewsService {
    private static final List<FeedSource> FEEDS = List.of(
            new FeedSource("BBC World", URI.create("https://feeds.bbci.co.uk/news/world/rss.xml")),
            new FeedSource("NPR", URI.create("https://feeds.npr.org/1001/rss.xml")),
            new FeedSource("NYTimes Home", URI.create("https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml"))
    );

    private final HttpClient client;
    private final TopicClassifier topicClassifier;

    public NewsService(TopicClassifier topicClassifier) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        this.topicClassifier = topicClassifier;
    }

    public List<Article> fetchArticles(TimeWindow window, Set<String> requestedTopics) {
        Set<String> seen = new HashSet<>();
        List<Article> all = new ArrayList<>();

        for (FeedSource feed : FEEDS) {
            try {
                HttpRequest request = HttpRequest.newBuilder(feed.uri())
                        .timeout(Duration.ofSeconds(12))
                        .header("User-Agent", "jnews/0.1")
                        .GET()
                        .build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 400) {
                    System.err.printf("Skipping %s: HTTP %d%n", feed.name(), response.statusCode());
                    continue;
                }
                List<Article> parsed = parseRss(response.body(), feed.name());
                for (Article article : parsed) {
                    if (article.published().isBefore(window.from()) || article.published().isAfter(window.to())) {
                        continue;
                    }
                    if (!requestedTopics.isEmpty() && !topicClassifier.matchesRequestedTopics(article, requestedTopics)) {
                        continue;
                    }
                    String dedupeKey = normalizeKey(article.link().isBlank() ? article.title() : article.link());
                    if (seen.add(dedupeKey)) {
                        all.add(article);
                    }
                }
            } catch (Exception e) {
                System.err.printf("Skipping %s: %s%n", feed.name(), e.getMessage());
            }
        }

        return all.stream()
                .sorted(Comparator.comparing(Article::published).reversed())
                .toList();
    }

    private String normalizeKey(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private List<Article> parseRss(InputStream in, String sourceName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder().parse(in);
        NodeList items = doc.getElementsByTagName("item");
        List<Article> out = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Node node = items.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element item = (Element) node;
            String title = firstText(item, "title");
            if (title.isBlank()) {
                continue;
            }
            Optional<Instant> published = parsePubDate(firstText(item, "pubDate"));
            if (published.isEmpty()) {
                continue;
            }
            String description = firstText(item, "description");
            out.add(new Article(
                    sourceName,
                    title,
                    firstText(item, "link"),
                    description,
                    published.get(),
                    topicClassifier.classify(title, description)
            ));
        }
        return out;
    }

    private String firstText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            return "";
        }
        Node node = list.item(0);
        return node == null ? "" : node.getTextContent().trim();
    }

    private Optional<Instant> parsePubDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        List<DateTimeFormatter> formats = Arrays.asList(
                DateTimeFormatter.RFC_1123_DATE_TIME,
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm z", Locale.ENGLISH),
                DateTimeFormatter.ISO_DATE_TIME
        );
        for (DateTimeFormatter fmt : formats) {
            try {
                return Optional.of(ZonedDateTime.parse(raw.trim(), fmt).toInstant());
            } catch (DateTimeParseException ignored) {
            }
        }
        try {
            return Optional.of(Instant.parse(raw.trim()));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }
}
