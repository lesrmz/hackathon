package com.prettypetty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prettypetty.dto.NotePatchRequest;
import com.prettypetty.exception.ApiException;
import com.prettypetty.model.PlannerBoard;
import com.prettypetty.repository.PlannerBoardRepository;
import com.prettypetty.repository.PlannerNoteRepository;
import com.prettypetty.repository.ThemeRepository;
import com.prettypetty.repository.UserRepository;
import com.prettypetty.repository.VillainProfileRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlannerServiceTest {
    private PlannerBoardRepository boardRepository;
    private PlannerNoteRepository noteRepository;
    private PlannerService plannerService;

    @BeforeEach
    void setUp() {
        boardRepository = org.mockito.Mockito.mock(PlannerBoardRepository.class);
        noteRepository = org.mockito.Mockito.mock(PlannerNoteRepository.class);
        plannerService = new PlannerService(
                boardRepository,
                noteRepository,
                org.mockito.Mockito.mock(VillainProfileRepository.class),
                org.mockito.Mockito.mock(ThemeRepository.class),
                org.mockito.Mockito.mock(UserRepository.class),
                org.mockito.Mockito.mock(AestheticService.class));
    }

    @Test
    void refusesToUpdateANoteThatDoesNotBelongToTheUrlBoard() {
        when(boardRepository.findByIdAndUserId("board-a", "user-a")).thenReturn(Optional.of(
                new PlannerBoard("board-a", "user-a", "pink-manipulator", "pink", Instant.now())));
        when(noteRepository.findByIdAndBoardIdAndUserId("note-b", "board-a", "user-a")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                plannerService.updateNote("user-a", "board-a", "note-b", new NotePatchRequest(null, null, null, true, null, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(noteRepository).findByIdAndBoardIdAndUserId("note-b", "board-a", "user-a");
    }

    @Test
    void rejectsBlankTextWhenEditingANote() {
        when(boardRepository.findByIdAndUserId("board-a", "user-a")).thenReturn(Optional.of(
                new PlannerBoard("board-a", "user-a", "pink-manipulator", "pink", Instant.now())));

        ApiException exception = assertThrows(ApiException.class, () ->
                plannerService.updateNote("user-a", "board-a", "note-a", new NotePatchRequest("  ", null, null, null, null, null, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void doesNotAllowAUserToLoadAnotherUsersBoard() {
        when(boardRepository.findByIdAndUserId("board-a", "user-b")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                plannerService.getBoard("user-b", "board-a"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
