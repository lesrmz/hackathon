package com.prettypetty.dto;

import java.util.List;

public record QuizQuestionResponse(String id, String text, List<AnswerChoice> answers) {
    public record AnswerChoice(String id, String text) {
    }
}
