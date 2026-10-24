package com.prettypetty.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBoardRequest(@NotBlank String villainId, @NotBlank String themeId) {
}
