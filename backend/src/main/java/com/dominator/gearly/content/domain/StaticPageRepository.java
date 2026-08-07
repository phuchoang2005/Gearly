package com.dominator.gearly.content.domain;

import java.util.Optional;

/** The port for reading standing pages. Implemented by {@code content.infrastructure}. */
public interface StaticPageRepository {

    Optional<StaticPage> findBySlug(String slug);
}
