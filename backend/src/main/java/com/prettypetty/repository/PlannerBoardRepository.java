package com.prettypetty.repository;

import com.prettypetty.model.PlannerBoard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlannerBoardRepository extends MongoRepository<PlannerBoard, String> {
    List<PlannerBoard> findByUserId(String userId);
    Optional<PlannerBoard> findByIdAndUserId(String id, String userId);
}
