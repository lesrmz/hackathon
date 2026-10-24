package com.prettypetty.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("villain_profiles")
public record VillainProfile(
        @Id String id,
        String name,
        String description,
        String signatureQuote,
        String outfitImage,
        String outfitDescription,
        String catStickerPath,
        String suggestedAestheticId,
        java.util.List<java.util.Map<String, Object>> collages) {
}
