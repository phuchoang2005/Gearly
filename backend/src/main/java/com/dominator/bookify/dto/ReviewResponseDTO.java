package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponseDTO {
    private String id;
    private int rating;
    private String subject;
    private String comment;
    private String addedAt;
    private String userName;
}
