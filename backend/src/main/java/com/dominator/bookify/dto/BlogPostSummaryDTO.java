package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostSummaryDTO {
    private String id;
    private String title;
    private String author;
    private Instant publishDate;
    private List<String> tags;
}