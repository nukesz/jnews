package org.example.jnews;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TopicClassifier {
    private final LinkedHashMap<String, List<String>> keywords = new LinkedHashMap<>();

    public TopicClassifier() {
        keywords.put("World", List.of("world", "europe", "asia", "africa", "middle east", "international"));
        keywords.put("Politics", List.of("politic", "election", "senate", "congress", "parliament", "white house"));
        keywords.put("Business", List.of("market", "economy", "inflation", "stock", "earnings", "business"));
        keywords.put("Technology", List.of("tech", "ai", "artificial intelligence", "software", "chip", "startup"));
        keywords.put("Health", List.of("health", "disease", "hospital", "vaccine", "medicine"));
        keywords.put("Science", List.of("science", "research", "space", "nasa", "climate"));
        keywords.put("Sports", List.of("sport", "football", "soccer", "nba", "nfl", "baseball", "tennis"));
        keywords.put("Conflict", List.of("war", "attack", "military", "conflict", "missile", "ceasefire"));
    }

    public String classify(String title, String description) {
        String text = (title + " " + description).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (matchesKeyword(text, keyword.toLowerCase(Locale.ROOT))) {
                    return entry.getKey();
                }
            }
        }
        return "General";
    }

    public boolean matchesRequestedTopics(Article article, Set<String> requestedTopics) {
        String haystack = (article.title() + " " + article.description() + " " + article.topic())
                .toLowerCase(Locale.ROOT);
        for (String topic : requestedTopics) {
            if (matchesKeyword(haystack, topic.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesKeyword(String text, String keyword) {
        if (keyword.contains(" ")) {
            return text.contains(keyword);
        }
        String regex = "(?<![a-z0-9])" + Pattern.quote(keyword) + "(?![a-z0-9])";
        return Pattern.compile(regex).matcher(text).find();
    }
}
