package com.dominator.gearly.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "categories")
public class Category {
    @Id
    private String id;
    private String name;
    private String description;
    private String parentId;

    /**
     * Real BSON dates, not ISO-8601 strings — the same normalization S7 applied to
     * {@code Product}. Stored as strings a category sorts lexicographically, which only
     * happens to be chronological while every value shares one format.
     * {@code data/seed/migrate.js} step 7 converts existing documents.
     */
    @CreatedDate
    private Instant addedAt;
    @LastModifiedDate
    private Instant modifiedAt;
}