package com.dominator.gearly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookInLowStockDTO {
    private String id;
    private String title;
    private int stock;
}
