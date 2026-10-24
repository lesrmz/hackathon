package com.prettypetty.repository;

import com.prettypetty.model.Reaction;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReactionRepository extends MongoRepository<Reaction, String> {
    List<Reaction> findByTriggerAndLanguage(String trigger, String language);
}
