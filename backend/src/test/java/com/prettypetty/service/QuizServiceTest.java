package com.prettypetty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prettypetty.dto.QuizResultRequest;
import com.prettypetty.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuizServiceTest {
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(new ObjectMapper());
    }

    @Test
    void questionsDoNotExposeScoringMappings() {
        var questions = quizService.getQuestions();
        assertEquals(5, questions.size());
        assertEquals(3, questions.get(0).answers().size());
    }

    @Test
    void resolvesTiedScoresUsingTheFixedPriorityOrder() {
        var result = quizService.getResult(new QuizResultRequest(List.of(
                "q1-pink", "q2-pink", "q3-corporate", "q4-romantic", "q5-corporate")));
        assertEquals("pink-manipulator", result.villainId());
        assertEquals("pink", result.suggestedThemeId());
    }

    @Test
    void rejectsMissingDuplicateAndUnknownAnswers() {
        assertThrows(ApiException.class, () -> quizService.getResult(
                new QuizResultRequest(List.of("q1-pink", "q2-pink"))));
        assertThrows(ApiException.class, () -> quizService.getResult(
                new QuizResultRequest(List.of("q1-pink", "q1-pink", "q2-pink", "q3-corporate", "q4-midnight"))));
        assertThrows(ApiException.class, () -> quizService.getResult(
                new QuizResultRequest(List.of("q1-pink", "q2-pink", "q3-corporate", "q4-romantic", "bogus"))));
    }
}
