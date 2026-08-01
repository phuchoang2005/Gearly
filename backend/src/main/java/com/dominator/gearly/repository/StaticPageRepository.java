package com.dominator.gearly.repository;

import com.dominator.gearly.model.StaticPage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaticPageRepository extends MongoRepository<StaticPage, String> {
    Optional<StaticPage> findBySlug(String slug);
}