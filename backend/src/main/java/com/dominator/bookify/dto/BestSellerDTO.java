package com.dominator.bookify.dto;

import lombok.Data;
import java.util.List;

@Data
public class BestSellerDTO {
    private String bookId;
    private String title;
    private List<String> authors;
    private double price;
    private int totalSold;
}
