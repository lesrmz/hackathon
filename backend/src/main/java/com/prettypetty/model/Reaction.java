package com.prettypetty.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("reactions")
public record Reaction(
        @Id String id,
        String text,
        String englishMeaning,
        String language,
        String direction,
        String trigger) {
}
