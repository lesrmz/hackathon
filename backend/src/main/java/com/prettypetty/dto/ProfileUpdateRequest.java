package com.prettypetty.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(@NotBlank String villainId, @NotBlank String aestheticId) {
}
