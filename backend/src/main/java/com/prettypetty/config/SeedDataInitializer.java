package com.prettypetty.config;

import com.prettypetty.model.Reaction;
import com.prettypetty.model.Theme;
import com.prettypetty.repository.ReactionRepository;
import com.prettypetty.repository.ThemeRepository;
import com.prettypetty.repository.VillainProfileRepository;
import com.prettypetty.service.QuizService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class SeedDataInitializer implements ApplicationRunner {
    private final QuizService quizService;
    private final VillainProfileRepository villainRepository;
    private final ThemeRepository themeRepository;
    private final ReactionRepository reactionRepository;
    private final ObjectMapper objectMapper;

    public SeedDataInitializer(
            QuizService quizService,
            VillainProfileRepository villainRepository,
            ThemeRepository themeRepository,
            ReactionRepository reactionRepository,
            ObjectMapper objectMapper) {
        this.quizService = quizService;
        this.villainRepository = villainRepository;
        this.themeRepository = themeRepository;
        this.reactionRepository = reactionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            villainRepository.saveAll(quizService.villainProfiles());
            themeRepository.saveAll(themes());
            reactionRepository.saveAll(reactionsFromFile());
        } catch (DataAccessException exception) {
            throw new IllegalStateException(
                    "Pretty Petty could not connect to MongoDB. Start MongoDB Community Server and check MONGODB_URI (default: mongodb://localhost:27017/pretty_petty).",
                    exception);
        }
    }

    private List<Theme> themes() {
        return List.of(
                new Theme("pink", "Main Character Pink", "#fff0f5", "#ffe5a4", "#30252d", "#da447a", "bows-hearts"),
                new Theme("red", "Petty in Red", "#fff0ec", "#ffd4c8", "#382421", "#c43c3d", "hearts-stars"),
                new Theme("purple", "Velvet Drama", "#f4efff", "#eadcf5", "#30243b", "#7950a3", "stars-moons"));
    }

    private List<Reaction> reactionsFromFile() {
        try {
            JsonNode captions = objectMapper.readTree(new ClassPathResource("reaction-data.json").getInputStream());
            java.util.ArrayList<Reaction> reactions = new java.util.ArrayList<>();
            for (JsonNode caption : captions) {
                String trigger = caption.path("trigger").asText();
                String language = caption.path("language").asText();
                reactions.add(reaction(
                        trigger, language, caption.path("text").asText(),
                        caption.path("meaning").asText()));
            }
            return reactions;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the fixed reaction data file.", exception);
        }
    }

    private Reaction reaction(String trigger, String language, String text, String meaning) {
        String direction = List.of("ary", "ar", "ur").contains(language) ? "rtl" : "ltr";
        return new Reaction(trigger + "-" + language, text, meaning, language, direction, trigger);
    }
}
