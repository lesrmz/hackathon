package com.prettypetty.controller;

import com.prettypetty.dto.BoardResponse;
import com.prettypetty.dto.CreateBoardRequest;
import com.prettypetty.dto.NotePatchRequest;
import com.prettypetty.dto.NoteRequest;
import com.prettypetty.dto.ThemeRequest;
import com.prettypetty.dto.VillainRequest;
import com.prettypetty.model.PlannerBoard;
import com.prettypetty.model.PlannerNote;
import com.prettypetty.service.PlannerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/boards")
public class PlannerController {
    private final PlannerService plannerService;

    public PlannerController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(@Valid @RequestBody CreateBoardRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plannerService.createBoard(authentication.getName(), request));
    }

    @GetMapping("/{boardId}")
    public BoardResponse getBoard(@PathVariable String boardId, Authentication authentication) {
        return plannerService.getBoard(authentication.getName(), boardId);
    }

    @PatchMapping("/{boardId}/theme")
    public PlannerBoard changeTheme(@PathVariable String boardId, @Valid @RequestBody ThemeRequest request, Authentication authentication) {
        return plannerService.changeTheme(authentication.getName(), boardId, request);
    }

    @PatchMapping("/{boardId}/villain")
    public PlannerBoard changeVillain(@PathVariable String boardId, @Valid @RequestBody VillainRequest request, Authentication authentication) {
        return plannerService.changeVillain(authentication.getName(), boardId, request);
    }

    @PostMapping("/{boardId}/notes")
    public ResponseEntity<PlannerNote> addNote(@PathVariable String boardId, @Valid @RequestBody NoteRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plannerService.addNote(authentication.getName(), boardId, request));
    }

    @PatchMapping("/{boardId}/notes/{noteId}")
    public PlannerNote updateNote(
            @PathVariable String boardId,
            @PathVariable String noteId,
            @Valid @RequestBody NotePatchRequest request,
            Authentication authentication) {
        return plannerService.updateNote(authentication.getName(), boardId, noteId, request);
    }

    @DeleteMapping("/{boardId}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String boardId, @PathVariable String noteId, Authentication authentication) {
        plannerService.deleteNote(authentication.getName(), boardId, noteId);
        return ResponseEntity.noContent().build();
    }
}
