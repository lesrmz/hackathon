package com.prettypetty.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("planner_notes")
public record PlannerNote(
        @Id String id,
        String boardId,
        String userId,
        String text,
        NoteType type,
        Instant deadline,
        String reactionText,
        String reactionMeaning,
        String reactionLanguage,
        String reactionDirection,
        boolean completed,
        double x,
        double y,
        double rotation,
        int zIndex) {
}
