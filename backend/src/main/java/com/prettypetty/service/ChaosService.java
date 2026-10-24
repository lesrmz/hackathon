package com.prettypetty.service;

import com.prettypetty.dto.ChaosRequest;
import com.prettypetty.exception.ApiException;
import com.prettypetty.model.PlannerNote;
import com.prettypetty.repository.PlannerBoardRepository;
import com.prettypetty.repository.PlannerNoteRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChaosService {
    private static final int NOTE_WIDTH = 200;
    private static final int NOTE_HEIGHT = 194;
    private final PlannerBoardRepository boardRepository;
    private final PlannerNoteRepository noteRepository;

    public ChaosService(PlannerBoardRepository boardRepository, PlannerNoteRepository noteRepository) {
        this.boardRepository = boardRepository;
        this.noteRepository = noteRepository;
    }

    public List<PlannerNote> makeWorse(String userId, String boardId, ChaosRequest request) {
        if (boardRepository.findByIdAndUserId(boardId, userId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Board not found. Check the link or create a new board.");
        }
        List<PlannerNote> notes = noteRepository.findByBoardIdAndUserId(boardId, userId);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int maxX = Math.max(0, request.width() - NOTE_WIDTH);
        int maxY = Math.max(0, request.height() - NOTE_HEIGHT);
        List<PlannerNote> shuffled = new ArrayList<>(notes);
        Collections.shuffle(shuffled, random);
        List<PlannerNote> updated = new ArrayList<>();
        for (int index = 0; index < shuffled.size(); index++) {
            PlannerNote note = shuffled.get(index);
            double x = request.mobile() ? note.x() : random.nextInt(maxX + 1);
            double y = request.mobile() ? note.y() : random.nextInt(maxY + 1);
            double rotation = random.nextDouble(-5.0, 5.01);
            updated.add(noteRepository.save(new PlannerNote(
                    note.id(), note.boardId(), note.userId(), note.text(), note.type(), note.deadline(),
                    note.reactionText(), note.reactionMeaning(), note.reactionLanguage(), note.reactionDirection(),
                    note.completed(), x, y, rotation, shuffled.size() - index)));
        }
        return updated;
    }
}
