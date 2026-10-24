package com.prettypetty.repository;

import com.prettypetty.model.VillainProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VillainProfileRepository extends MongoRepository<VillainProfile, String> {
}
