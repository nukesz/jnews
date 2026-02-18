package org.example.jnews;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;

public class ArticleReader {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public String readText(String link) {
        if (link == null || link.isBlank()) {
            return "";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(link))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "jnews/0.1")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                return "";
            }

            org.jsoup.nodes.Document doc = Jsoup.parse(response.body(), link);
            doc.select("script,style,noscript,header,footer,nav,aside,form").remove();

            Elements paragraphs = new Elements();
            Element article = doc.selectFirst("article");
            if (article != null) {
                paragraphs = article.select("p");
            }
            if (paragraphs.isEmpty()) {
                paragraphs = doc.select("main p");
            }
            if (paragraphs.isEmpty()) {
                paragraphs = doc.select("p");
            }

            LinkedHashSet<String> lines = new LinkedHashSet<>();
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() >= 40) {
                    lines.add(text);
                }
                if (lines.size() >= 80) {
                    break;
                }
            }
            return String.join("\n\n", lines);
        } catch (Exception ignored) {
            return "";
        }
    }
}
