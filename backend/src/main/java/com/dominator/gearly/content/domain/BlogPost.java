package com.dominator.gearly.content.domain;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * An editorial article, optionally tied to a product it reviews or recommends.
 *
 * <h2>Read-only, and that is the whole model</h2>
 * There is no write path: no admin CRUD, no endpoint, no service method that saves one.
 * Articles arrive with the seed data. So the aggregate has no behaviour to protect, and the
 * honest way to say that is to expose no way of changing it — which is also what
 * {@code aggregates_expose_no_public_setters} requires of anything in a {@code ..domain..}
 * package.
 *
 * <p>Dropping {@code @Data} was therefore not a cosmetic change. Lombok generated a public
 * setter for every field of a document nothing is supposed to modify, and an
 * {@code @AllArgsConstructor} that let one be built with no invariant checked at all. Spring
 * Data does not need either: it writes fields reflectively.
 *
 * <p>{@code productId} is a bare {@code String} rather than a {@link
 * com.dominator.gearly.shared.domain.ProductId} on purpose — see {@link #productId()}.
 */
@Getter
@Document(collection = "blogPosts")
public class BlogPost {

    @Id
    private String id;

    private String title;

    private String author;

    private Instant publishDate;

    @Field("productId")
    private String productId;

    private List<String> tags;

    private String content;

    /** For Spring Data. */
    protected BlogPost() {
    }

    /**
     * The product this article is about, or {@code null}.
     *
     * <p>Deliberately not a typed {@code ProductId}. Content is a downstream reader that never
     * resolves the reference — it hands the raw value to the storefront, which fetches the
     * product itself — and adopting the type would change the field's stored form for a link
     * this context does not follow.
     */
    public String productId() {
        return productId;
    }

    /** Never {@code null}, so a caller need not guard a post that was seeded without tags. */
    public List<String> getTags() {
        return tags == null ? List.of() : Collections.unmodifiableList(tags);
    }
}
