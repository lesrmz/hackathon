package com.prettypetty.controller;

import com.prettypetty.dto.ChaosRequest;
import com.prettypetty.model.PlannerNote;
import com.prettypetty.service.ChaosService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards/{boardId}/chaos")
public class ChaosController {
    private final ChaosService chaosService;

    public ChaosController(ChaosService chaosService) {
        this.chaosService = chaosService;
    }

    @PostMapping
    public List<PlannerNote> makeWorse(
            @PathVariable String boardId,
            @Valid @RequestBody ChaosRequest request,
            Authentication authentication) {
        return chaosService.makeWorse(authentication.getName(), boardId, request);
    }
}
