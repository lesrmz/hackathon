package com.prettypetty.controller;

import com.prettypetty.model.Reaction;
import com.prettypetty.model.Theme;
import com.prettypetty.service.AestheticService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AestheticController {
    private final AestheticService aestheticService;

    public AestheticController(AestheticService aestheticService) {
        this.aestheticService = aestheticService;
    }

    @GetMapping("/themes")
    public List<Theme> themes() {
        return aestheticService.getThemes();
    }

    @GetMapping("/reactions")
    public Reaction reaction(@RequestParam String trigger, @RequestParam(required = false) String language) {
        return aestheticService.getReaction(trigger, language);
    }
}
