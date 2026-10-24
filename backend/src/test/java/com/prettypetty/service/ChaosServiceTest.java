package com.prettypetty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.prettypetty.dto.ChaosRequest;
import com.prettypetty.exception.ApiException;
import com.prettypetty.model.NoteType;
import com.prettypetty.model.PlannerBoard;
import com.prettypetty.model.PlannerNote;
import com.prettypetty.repository.PlannerBoardRepository;
import com.prettypetty.repository.PlannerNoteRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChaosServiceTest {
    private PlannerBoardRepository boardRepository;
    private PlannerNoteRepository noteRepository;
    private ChaosService chaosService;

    @BeforeEach
    void setUp() {
        boardRepository = org.mockito.Mockito.mock(PlannerBoardRepository.class);
        noteRepository = org.mockito.Mockito.mock(PlannerNoteRepository.class);
        chaosService = new ChaosService(boardRepository, noteRepository);
    }

    @Test
    void refusesToRearrangeAnotherUsersBoard() {
        when(boardRepository.findByIdAndUserId("board-a", "user-b")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                chaosService.makeWorse("user-b", "board-a", new ChaosRequest(500, 400, false)));

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void keepsDesktopPositionsWithinTheUsableBoard() {
        when(boardRepository.findByIdAndUserId("board-a", "user-a")).thenReturn(Optional.of(
                new PlannerBoard("board-a", "user-a", "pink-manipulator", "pink", Instant.now())));
        when(noteRepository.findByBoardIdAndUserId("board-a", "user-a")).thenReturn(List.of(note()));
        when(noteRepository.save(any(PlannerNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlannerNote updated = chaosService.makeWorse("user-a", "board-a", new ChaosRequest(300, 260, false)).get(0);

        assertTrue(updated.x() >= 0 && updated.x() <= 100);
        assertTrue(updated.y() >= 0 && updated.y() <= 66);
        assertTrue(updated.rotation() >= -5 && updated.rotation() <= 5);
        assertEquals(1, updated.zIndex());
    }

    @Test
    void preservesDesktopCoordinatesWhenShufflingMobileNotes() {
        PlannerNote original = note();
        when(boardRepository.findByIdAndUserId("board-a", "user-a")).thenReturn(Optional.of(
                new PlannerBoard("board-a", "user-a", "pink-manipulator", "pink", Instant.now())));
        when(noteRepository.findByBoardIdAndUserId("board-a", "user-a")).thenReturn(List.of(original));
        when(noteRepository.save(any(PlannerNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlannerNote updated = chaosService.makeWorse("user-a", "board-a", new ChaosRequest(100, 100, true)).get(0);

        assertEquals(original.x(), updated.x());
        assertEquals(original.y(), updated.y());
    }

    private PlannerNote note() {
        return new PlannerNote(
                "note-a", "board-a", "user-a", "Make it worse", NoteType.TASK, null,
                "One more task.", "One more task.", "en", "ltr", false, 27, 45, 0, 0);
    }
}
