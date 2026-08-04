package com.dominator.gearly.model;

import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "products")
public class Product {
    @Id
    private String id;

    /**
     * Optimistic-locking token. Closes the read-then-write oversell race in
     * {@code ProductService.decreaseStock}: two concurrent checkouts could both read stock
     * 1, both pass the check, and both write stock 0 — selling the same unit twice. With a
     * version, the second write matches no document and fails with
     * {@code OptimisticLockingFailureException}, which the global handler maps to 409.
     *
     * <p>{@code @JsonIgnore} because this is an internal concurrency token: it must never
     * appear on the wire, and a client must never be able to supply one. Mongo still
     * stores it — Jackson and the Mongo mapper are separate.
     *
     * <p>Boxed on purpose. Spring Data treats a {@code null} version as "not yet
     * persisted"; a document written before this field existed would therefore be
     * re-inserted rather than updated, so {@code data/seed/migrate.js} backfills 0.
     */
    @Version
    @JsonIgnore
    private Long version;
    private String title;
    private List<String> authors;
    private String description;

    /**
     * Prices default to {@link Money#ZERO} rather than {@code null} so that a document
     * written before the field existed reads back as {@code 0.00}, exactly as the previous
     * {@code double} did.
     */
    private Money price = Money.ZERO;
    private Money originalPrice = Money.ZERO;

    private ProductCondition condition;
    private int stock;

    /**
     * Stored as BSON {@code ObjectId} — see {@link CategoryId} for why this one id type is
     * different, and {@code DomainTypeConverters} for the pair that keeps it that way.
     */
    private List<CategoryId> categoryIds;

    private List<Image> images;

    @Transient
    private List<String> categoryNames;

    // Rating
    private double averageRating;
    private int ratingCount;
    private int totalRating;

    @CreatedDate
    private Instant addedAt;

    @LastModifiedDate
    private Instant modifiedAt;
}