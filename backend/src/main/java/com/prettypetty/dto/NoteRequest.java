package com.prettypetty.dto;

import com.prettypetty.model.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record NoteRequest(
        @NotBlank String text,
        @NotNull NoteType type,
        Instant deadline,
        Double x,
        Double y,
        Double rotation,
        Integer zIndex) {
}
