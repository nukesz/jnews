package org.example.jnews;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

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
            new FeedSource("NYTimes Home", URI.create("https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml")),
            new FeedSource("CNN Top Stories", URI.create("http://rss.cnn.com/rss/edition.rss")),
            new FeedSource("Fox News Latest", URI.create("https://moxie.foxnews.com/google-publisher/latest.xml")),
            new FeedSource("The Guardian World", URI.create("https://www.theguardian.com/world/rss")),
            new FeedSource("Al Jazeera", URI.create("https://www.aljazeera.com/xml/rss/all.xml")),
            new FeedSource("ABC News Top", URI.create("https://abcnews.go.com/abcnews/topstories")),
            new FeedSource("CBS News Latest", URI.create("https://www.cbsnews.com/latest/rss/main")),
            new FeedSource("CNBC Top News", URI.create("https://www.cnbc.com/id/100003114/device/rss/rss.html")),
            new FeedSource("WSJ World", URI.create("https://feeds.a.dj.com/rss/RSSWorldNews.xml")),
            new FeedSource("Politico Picks", URI.create("https://www.politico.com/rss/politicopicks.xml"))
    );

    private final HttpClient client;
    private final TopicClassifier topicClassifier;

    public NewsService(TopicClassifier topicClassifier) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
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
                        .header("Accept", "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
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
                System.err.printf("Skipping %s: %s%n", feed.name(), describeError(e));
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
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void warning(SAXParseException e) {
            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        });
        Document doc = builder.parse(in);
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

    private String describeError(Exception e) {
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
