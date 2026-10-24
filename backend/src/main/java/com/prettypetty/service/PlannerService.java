package com.prettypetty.service;

import com.prettypetty.dto.BoardResponse;
import com.prettypetty.dto.CreateBoardRequest;
import com.prettypetty.dto.NotePatchRequest;
import com.prettypetty.dto.NoteRequest;
import com.prettypetty.dto.ThemeRequest;
import com.prettypetty.dto.VillainRequest;
import com.prettypetty.exception.ApiException;
import com.prettypetty.model.NoteType;
import com.prettypetty.model.PlannerBoard;
import com.prettypetty.model.PlannerNote;
import com.prettypetty.repository.PlannerBoardRepository;
import com.prettypetty.repository.PlannerNoteRepository;
import com.prettypetty.repository.ThemeRepository;
import com.prettypetty.repository.UserRepository;
import com.prettypetty.repository.VillainProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PlannerService {
    private final PlannerBoardRepository boardRepository;
    private final PlannerNoteRepository noteRepository;
    private final VillainProfileRepository villainRepository;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;
    private final AestheticService aestheticService;

    public PlannerService(
            PlannerBoardRepository boardRepository,
            PlannerNoteRepository noteRepository,
            VillainProfileRepository villainRepository,
            ThemeRepository themeRepository,
            UserRepository userRepository,
            AestheticService aestheticService) {
        this.boardRepository = boardRepository;
        this.noteRepository = noteRepository;
        this.villainRepository = villainRepository;
        this.themeRepository = themeRepository;
        this.userRepository = userRepository;
        this.aestheticService = aestheticService;
    }

    public BoardResponse createBoard(String userId, CreateBoardRequest request) {
        if (!villainRepository.existsById(request.villainId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid quiz villain before creating a board.");
        }
        if (!themeRepository.existsById(request.themeId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose one of the available board themes.");
        }
        PlannerBoard board = new PlannerBoard(UUID.randomUUID().toString(), userId, request.villainId(), request.themeId(), Instant.now());
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
        userRepository.save(new com.prettypetty.model.User(
                user.id(), user.email(), user.passwordHash(), user.displayName(), request.villainId(), request.themeId()));
        return new BoardResponse(boardRepository.save(board), List.of());
    }

    public BoardResponse getBoard(String userId, String boardId) {
        PlannerBoard board = requireBoard(userId, boardId);
        return new BoardResponse(board, noteRepository.findByBoardIdAndUserId(boardId, userId));
    }

    public PlannerBoard changeTheme(String userId, String boardId, ThemeRequest request) {
        PlannerBoard board = requireBoard(userId, boardId);
        if (!themeRepository.existsById(request.themeId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose one of the available board themes.");
        }
        PlannerBoard updated = boardRepository.save(new PlannerBoard(board.id(), userId, board.villainId(), request.themeId(), board.createdAt()));
        userRepository.findById(userId).ifPresent(user -> userRepository.save(new com.prettypetty.model.User(
                user.id(), user.email(), user.passwordHash(), user.displayName(), user.villainId(), request.themeId())));
        return updated;
    }

    public PlannerBoard changeVillain(String userId, String boardId, VillainRequest request) {
        PlannerBoard board = requireBoard(userId, boardId);
        if (!villainRepository.existsById(request.villainId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid quiz villain.");
        }
        PlannerBoard updated = boardRepository.save(
                new PlannerBoard(board.id(), userId, request.villainId(), board.themeId(), board.createdAt()));
        userRepository.findById(userId).ifPresent(user -> userRepository.save(new com.prettypetty.model.User(
                user.id(), user.email(), user.passwordHash(), user.displayName(), request.villainId(), user.aestheticId())));
        return updated;
    }

    public PlannerNote addNote(String userId, String boardId, NoteRequest request) {
        requireBoard(userId, boardId);
        double x = Math.max(0, request.x() == null ? 24 : request.x());
        double y = Math.max(0, request.y() == null ? 24 : request.y());
        double rotation = clamp(request.rotation() == null ? 0 : request.rotation(), -8, 8);
        int zIndex = Math.max(0, request.zIndex() == null ? 0 : request.zIndex());
        var reaction = aestheticService.getReaction("ADD_NOTE", null);
        return noteRepository.save(new PlannerNote(
                UUID.randomUUID().toString(), boardId, userId, request.text().trim(), request.type(),
                request.deadline(), reaction.text(), reaction.englishMeaning(), reaction.language(), reaction.direction(),
                false, x, y, rotation, zIndex));
    }

    public PlannerNote updateNote(String userId, String boardId, String noteId, NotePatchRequest request) {
        requireBoard(userId, boardId);
        if (request.text() != null && request.text().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Note text cannot be blank.");
        }
        PlannerNote note = noteRepository.findByIdAndBoardIdAndUserId(noteId, boardId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Note not found on this board."));
        return noteRepository.save(new PlannerNote(
                note.id(),
                note.boardId(),
                note.userId(),
                request.text() == null ? note.text() : request.text().trim(),
                request.type() == null ? note.type() : request.type(),
                request.deadline() == null && request.text() == null ? note.deadline() : request.deadline(),
                note.reactionText(),
                note.reactionMeaning(),
                note.reactionLanguage(),
                note.reactionDirection(),
                request.completed() == null ? note.completed() : request.completed(),
                request.x() == null ? note.x() : Math.max(0, request.x()),
                request.y() == null ? note.y() : Math.max(0, request.y()),
                request.rotation() == null ? note.rotation() : clamp(request.rotation(), -8, 8),
                request.zIndex() == null ? note.zIndex() : Math.max(0, request.zIndex())));
    }

    public void deleteNote(String userId, String boardId, String noteId) {
        requireBoard(userId, boardId);
        PlannerNote note = noteRepository.findByIdAndBoardIdAndUserId(noteId, boardId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Note not found on this board."));
        noteRepository.delete(note);
    }

    private PlannerBoard requireBoard(String userId, String boardId) {
        return boardRepository.findByIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Board not found. Check the link or create a new board."));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
