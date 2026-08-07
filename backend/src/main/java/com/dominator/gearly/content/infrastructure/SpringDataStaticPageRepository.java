package com.dominator.gearly.content.infrastructure;

import com.dominator.gearly.content.domain.StaticPage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Spring Data's view of the pages collection. Reached only through {@link MongoStaticPageRepository}. */
interface SpringDataStaticPageRepository extends MongoRepository<StaticPage, String> {

    Optional<StaticPage> findBySlug(String slug);
}
