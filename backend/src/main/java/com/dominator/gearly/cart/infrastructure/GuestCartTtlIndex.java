package com.dominator.gearly.cart.infrastructure;

import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.util.concurrent.TimeUnit;

/**
 * Expires abandoned guest carts seven days after their last update.
 *
 * <p>A partial index, so it applies only to documents that have a {@code guestId} — a signed-in
 * customer's cart is theirs until they empty it.
 *
 * <h2>Why this is cart infrastructure and not platform config</h2>
 * It was {@code config.dbConfig.GuestCartTTLConfig}. When S13 moved the {@code config} package
 * under {@code platform}, {@code mongo_template_is_reserved_for_analytics_and_adapters} started
 * failing on it — correctly. The rule permits {@code MongoTemplate} in the read side and in
 * repository adapters, and this is neither a general piece of wiring nor an exception to be
 * carved out: it is a statement about how one context's collection is stored, which is exactly
 * what an infrastructure package is for. The cart owns the {@code carts} collection, so the
 * cart owns its indexes.
 */
@Configuration
@RequiredArgsConstructor
public class GuestCartTtlIndex {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void createGuestCartTTLIndex() {
        IndexOptions options = new IndexOptions()
                .expireAfter(7L, TimeUnit.DAYS)
                .name("guestCartTTLIndex")
                .partialFilterExpression(new Document("guestId", new Document("$exists", true)));

        Index index = new Index().on("updatedAt", Sort.Direction.ASC);

        mongoTemplate
                .getCollection("carts")
                .createIndex(index.getIndexKeys(), options);
    }
}
