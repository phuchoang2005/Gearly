package com.dominator.gearly.content.infrastructure;

import com.dominator.gearly.content.domain.StaticPage;
import com.dominator.gearly.content.domain.StaticPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** The {@link StaticPageRepository} adapter. */
@Repository
@RequiredArgsConstructor
public class MongoStaticPageRepository implements StaticPageRepository {

    private final SpringDataStaticPageRepository pages;

    @Override
    public Optional<StaticPage> findBySlug(String slug) {
        return pages.findBySlug(slug);
    }
}
