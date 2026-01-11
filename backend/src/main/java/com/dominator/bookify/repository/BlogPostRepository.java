package com.dominator.bookify.repository;

import com.dominator.bookify.model.BlogPost;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostRepository extends MongoRepository<BlogPost, String> {
}