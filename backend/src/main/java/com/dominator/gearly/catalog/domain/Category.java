package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.CategoryId;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * How the catalog is organized. A small aggregate — it owns only its own name and its place in
 * the tree — but an aggregate rather than a lookup table, because a product references it by
 * {@link CategoryId} and nothing else may reach inside it.
 *
 * <p>The timestamps are real BSON dates rather than the ISO-8601 strings they used to be; S9
 * converted them and {@code data/seed/migrate.js} step 7 carries existing documents across.
 * Stored as strings, a category sorted lexicographically, which only happens to be
 * chronological while every value shares one format.
 */
@Getter
@Document(collection = "categories")
public class Category {

    @Id
    private String id;

    private String name;
    private String description;
    private String parentId;

    @CreatedDate
    private Instant addedAt;
    @LastModifiedDate
    private Instant modifiedAt;

    /** For Spring Data. */
    private Category() {
    }

    public static Category create(String name, String description, String parentId) {
        Category category = new Category();
        category.name = name;
        category.description = description;
        category.parentId = parentId;
        Instant now = Instant.now();
        category.addedAt = now;
        category.modifiedAt = now;
        return category;
    }

    /** The typed identity. Null until Mongo has assigned one on first save. */
    public CategoryId categoryId() {
        return id == null ? null : CategoryId.of(id);
    }

    public void rename(String name, String description) {
        this.name = name;
        this.description = description;
        this.modifiedAt = Instant.now();
    }
}
