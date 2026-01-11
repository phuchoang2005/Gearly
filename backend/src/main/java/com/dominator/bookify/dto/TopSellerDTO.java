package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopSellerDTO {
    private String bookId;
    private String title;
    private long totalSold;
}
