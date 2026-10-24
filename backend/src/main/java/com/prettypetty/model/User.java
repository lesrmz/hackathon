package com.prettypetty.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public record User(
        @Id String id,
        @Indexed(unique = true) String email,
        String passwordHash,
        String displayName,
        String villainId,
        String aestheticId) {
}
