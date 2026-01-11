package com.dominator.bookify.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pages")
public class StaticPage {
    @Id
    private String id;
    private String title;
    private String slug;
    private String content;
    private Instant lastModified;
}