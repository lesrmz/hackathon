package com.prettypetty.dto;

import jakarta.validation.constraints.NotBlank;

public record VillainRequest(@NotBlank String villainId) {
}
