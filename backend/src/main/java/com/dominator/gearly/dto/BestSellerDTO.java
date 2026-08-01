package com.dominator.gearly.dto;

import lombok.Data;
import java.util.List;

@Data
public class BestSellerDTO {
    private String productId;
    private String title;
    private List<String> authors;
    private double price;
    private int totalSold;
}
