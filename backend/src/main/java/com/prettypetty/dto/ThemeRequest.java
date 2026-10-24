package com.prettypetty.dto;

import jakarta.validation.constraints.NotBlank;

public record ThemeRequest(@NotBlank String themeId) {
}
