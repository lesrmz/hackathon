package com.prettypetty.repository;

import com.prettypetty.model.PlannerNote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlannerNoteRepository extends MongoRepository<PlannerNote, String> {
    List<PlannerNote> findByBoardIdAndUserId(String boardId, String userId);
    Optional<PlannerNote> findByIdAndBoardIdAndUserId(String id, String boardId, String userId);
}
