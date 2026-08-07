package com.dominator.gearly.content.domain;

import java.util.List;
import java.util.Optional;

/** The port for reading articles. Implemented by {@code content.infrastructure}. */
public interface BlogPostRepository {

    List<BlogPost> findAll();

    Optional<BlogPost> findById(String id);
}
