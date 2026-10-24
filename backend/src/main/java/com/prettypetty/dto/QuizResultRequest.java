package com.prettypetty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record QuizResultRequest(@NotEmpty List<@NotBlank String> answerIds) {
}
