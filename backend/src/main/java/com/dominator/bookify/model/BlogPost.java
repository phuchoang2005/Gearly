package com.dominator.bookify.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "blogPosts")
public class BlogPost {

    @Id
    private String id;

    private String title;

    private String author;

    private Instant publishDate;

    @Field("bookId")
    private String bookId;

    private List<String> tags;

    private String content;
}