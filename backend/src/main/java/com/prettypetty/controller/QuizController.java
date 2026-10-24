package com.prettypetty.controller;

import com.prettypetty.dto.QuizQuestionResponse;
import com.prettypetty.dto.QuizResultRequest;
import com.prettypetty.dto.QuizResultResponse;
import com.prettypetty.service.QuizService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/questions")
    public List<QuizQuestionResponse> questions() {
        return quizService.getQuestions();
    }

    @PostMapping("/result")
    public QuizResultResponse result(@Valid @RequestBody QuizResultRequest request) {
        return quizService.getResult(request);
    }

    @GetMapping("/villains/{villainId}")
    public QuizResultResponse villain(@PathVariable String villainId) {
        return quizService.getVillain(villainId);
    }
}
