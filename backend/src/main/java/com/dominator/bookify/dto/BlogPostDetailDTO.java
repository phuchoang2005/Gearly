package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostDetailDTO {
    private String id;
    private String title;
    private String author;
    private Instant publishDate;
    private String bookId;
    private List<String> tags;
    private String content;
}