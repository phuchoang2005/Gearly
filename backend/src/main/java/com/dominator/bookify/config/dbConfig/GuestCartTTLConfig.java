package com.dominator.bookify.config.dbConfig;

import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class GuestCartTTLConfig {

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
