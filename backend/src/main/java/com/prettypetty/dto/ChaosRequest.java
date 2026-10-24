package com.prettypetty.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChaosRequest(
        @NotNull @Positive @Max(10000) Integer width,
        @NotNull @Positive @Max(10000) Integer height,
        boolean mobile) {
}
