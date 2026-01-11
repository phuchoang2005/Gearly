package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaticPageDTO {
    private String id;
    private String title;
    private String slug;
    private String content;
    private Instant lastModified;
}