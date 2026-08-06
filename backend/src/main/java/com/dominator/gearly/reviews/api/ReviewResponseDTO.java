package com.dominator.gearly.reviews.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
    private Instant addedAt;
    private String userName;
}
