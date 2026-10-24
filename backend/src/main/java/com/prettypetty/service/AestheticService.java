package com.prettypetty.service;

import com.prettypetty.exception.ApiException;
import com.prettypetty.model.Reaction;
import com.prettypetty.model.Theme;
import com.prettypetty.repository.ReactionRepository;
import com.prettypetty.repository.ThemeRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AestheticService {
    private static final List<String> LANGUAGES = List.of("en", "ary", "ar", "fr", "es", "ur", "ru", "ky");
    private static final Set<String> TRIGGERS = Set.of("ADD_NOTE", "COMPLETE", "CHAOS", "QUIZ_START", "RESULT", "PLANNER_ENTRY");
    private final ThemeRepository themeRepository;
    private final ReactionRepository reactionRepository;

    public AestheticService(ThemeRepository themeRepository, ReactionRepository reactionRepository) {
        this.themeRepository = themeRepository;
        this.reactionRepository = reactionRepository;
    }

    public List<Theme> getThemes() {
        return themeRepository.findAll();
    }

    public Reaction getReaction(String trigger, String requestedLanguage) {
        if (trigger == null || !TRIGGERS.contains(trigger)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reaction trigger is not supported.");
        }
        if (requestedLanguage != null && !LANGUAGES.contains(requestedLanguage)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reaction language is not supported.");
        }
        String language = requestedLanguage == null
                ? LANGUAGES.get(ThreadLocalRandom.current().nextInt(LANGUAGES.size()))
                : requestedLanguage;
        List<Reaction> choices = reactionRepository.findByTriggerAndLanguage(trigger, language);
        if (choices.isEmpty()) {
            throw new IllegalStateException("No seeded reaction exists for " + trigger + " in language " + language + ".");
        }
        return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
    }
}
