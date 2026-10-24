package com.prettypetty.dto;

public record QuizResultResponse(
        String villainId,
        String name,
        String description,
        String signatureQuote,
        String outfitImage,
        String outfitDescription,
        String suggestedThemeId,
        String suggestedAestheticId,
        String catStickerPath,
        java.util.List<java.util.Map<String, Object>> collages) {
}
