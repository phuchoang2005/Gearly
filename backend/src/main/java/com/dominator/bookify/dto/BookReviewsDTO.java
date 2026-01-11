package com.dominator.bookify.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookReviewsDTO {
    String bookId;
    int pageSize;
    int pageIndex;
    int rating = 0;
    String sortBy = "addedAt";
}
