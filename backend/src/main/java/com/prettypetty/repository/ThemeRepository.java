package com.prettypetty.repository;

import com.prettypetty.model.Theme;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ThemeRepository extends MongoRepository<Theme, String> {
}
