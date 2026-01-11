package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuantitySoldDTO {
    private String bookId;
    private long totalSold;
    private String title;
}
