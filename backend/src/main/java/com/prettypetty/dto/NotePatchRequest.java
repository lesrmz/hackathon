package com.prettypetty.dto;

import com.prettypetty.model.NoteType;
import java.time.Instant;

public record NotePatchRequest(
        String text,
        NoteType type,
        Instant deadline,
        Boolean completed,
        Double x,
        Double y,
        Double rotation,
        Integer zIndex) {
}
