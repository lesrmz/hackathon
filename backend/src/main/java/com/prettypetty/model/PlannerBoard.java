package com.prettypetty.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("planner_boards")
public record PlannerBoard(
        @Id String id,
        String userId,
        String villainId,
        String themeId,
        Instant createdAt) {
}
