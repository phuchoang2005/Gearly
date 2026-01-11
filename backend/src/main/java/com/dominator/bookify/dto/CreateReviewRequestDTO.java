package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateReviewRequestDTO {
    private String bookId;
    private String subject;
    private String comment;
    private int rating;
}
