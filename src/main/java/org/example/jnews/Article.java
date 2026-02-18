package org.example.jnews;

import java.time.Instant;

public record Article(
        String source,
        String title,
        String link,
        String description,
        Instant published,
        String topic
) {
    public Article {
        description = description == null ? "" : description;
        link = link == null ? "" : link;
    }
}
