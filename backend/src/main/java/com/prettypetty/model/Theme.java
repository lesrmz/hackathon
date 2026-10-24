package com.prettypetty.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("themes")
public record Theme(
        @Id String id,
        String name,
        String backgroundColor,
        String noteColor,
        String textColor,
        String accentColor,
        String decorationSet) {
}
