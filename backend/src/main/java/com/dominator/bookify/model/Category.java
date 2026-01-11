package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

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

    @CreatedDate
    private String addedAt;
    @LastModifiedDate
    private String modifiedAt;
}