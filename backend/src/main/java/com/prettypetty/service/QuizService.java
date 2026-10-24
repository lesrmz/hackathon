package com.prettypetty.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prettypetty.dto.QuizQuestionResponse;
import com.prettypetty.dto.QuizResultRequest;
import com.prettypetty.dto.QuizResultResponse;
import com.prettypetty.exception.ApiException;
import com.prettypetty.model.VillainProfile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QuizService {
    private final JsonNode catalog;
    private final ObjectMapper objectMapper;

    public QuizService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        try {
            this.catalog = objectMapper.readTree(new ClassPathResource("quiz-data.json").getInputStream());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the fixed quiz data file.", exception);
        }
    }

    public List<QuizQuestionResponse> getQuestions() {
        List<QuizQuestionResponse> questions = new ArrayList<>();
        for (JsonNode question : catalog.path("questions")) {
            List<QuizQuestionResponse.AnswerChoice> answers = new ArrayList<>();
            for (JsonNode answer : question.path("answers")) {
                answers.add(new QuizQuestionResponse.AnswerChoice(
                        answer.path("id").asText(),
                        answer.path("text").asText()));
            }
            questions.add(new QuizQuestionResponse(
                    question.path("id").asText(),
                    question.path("text").asText(),
                    answers));
        }
        return questions;
    }

    public List<VillainProfile> villainProfiles() {
        List<VillainProfile> profiles = new ArrayList<>();
        for (JsonNode villain : catalog.path("villains")) {
            profiles.add(profile(villain));
        }
        return profiles;
    }

    public QuizResultResponse getResult(QuizResultRequest request) {
        int questionCount = catalog.path("questions").size();
        if (request.answerIds().size() != questionCount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Submit exactly one answer for each of the " + questionCount + " quiz questions.");
        }

        Map<String, String> answerToVillain = new HashMap<>();
        Map<String, String> answerToQuestion = new HashMap<>();
        for (JsonNode question : catalog.path("questions")) {
            for (JsonNode answer : question.path("answers")) {
                answerToVillain.put(answer.path("id").asText(), answer.path("villainId").asText());
                answerToQuestion.put(answer.path("id").asText(), question.path("id").asText());
            }
        }

        Set<String> answerIds = new HashSet<>();
        Set<String> answeredQuestions = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();
        for (String answerId : request.answerIds()) {
            if (!answerIds.add(answerId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate answer IDs are not allowed.");
            }
            String villainId = answerToVillain.get(answerId);
            String questionId = answerToQuestion.get(answerId);
            if (villainId == null || questionId == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "One or more answer IDs are not recognized.");
            }
            if (!answeredQuestions.add(questionId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Submit exactly one answer for each quiz question.");
            }
            scores.merge(villainId, 1, Integer::sum);
        }
        if (answeredQuestions.size() != questionCount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Submit exactly one answer for each quiz question.");
        }

        String winnerId = null;
        int highScore = -1;
        for (JsonNode priority : catalog.path("tiePriority")) {
            String villainId = priority.asText();
            int score = scores.getOrDefault(villainId, 0);
            if (score > highScore) {
                winnerId = villainId;
                highScore = score;
            }
        }

        for (JsonNode villain : catalog.path("villains")) {
            if (villain.path("id").asText().equals(winnerId)) {
                return response(villain);
            }
        }
        throw new IllegalStateException("Quiz tie priority references an unknown villain ID.");
    }

    public QuizResultResponse getVillain(String villainId) {
        for (JsonNode villain : catalog.path("villains")) {
            if (villain.path("id").asText().equals(villainId)) {
                return response(villain);
            }
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "Villain profile not found.");
    }

    private QuizResultResponse response(JsonNode villain) {
        String villainId = villain.path("id").asText();
        String suggestedTheme = villain.path("suggestedAestheticId").asText(defaultTheme(villainId));
        return new QuizResultResponse(
                villainId,
                villain.path("name").asText(),
                villain.path("description").asText(),
                villain.path("signatureQuote").asText(),
                villain.path("outfitImage").asText(),
                villain.path("outfitDescription").asText(),
                suggestedTheme,
                suggestedTheme,
                villain.path("catStickerPath").asText(),
                objectMapper.convertValue(villain.path("collages"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)));
    }

    private VillainProfile profile(JsonNode villain) {
        return new VillainProfile(
                villain.path("id").asText(),
                villain.path("name").asText(),
                villain.path("description").asText(),
                villain.path("signatureQuote").asText(),
                villain.path("outfitImage").asText(),
                villain.path("outfitDescription").asText(),
                villain.path("catStickerPath").asText(),
                villain.path("suggestedAestheticId").asText(),
                objectMapper.convertValue(villain.path("collages"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)));
    }

    private String defaultTheme(String villainId) {
        return switch (villainId) {
            case "pink-manipulator", "innocent-menace" -> "pink";
            case "corporate-heartbreaker" -> "red";
            default -> "purple";
        };
    }
}
