package com.dominator.bookify.dto;

import com.dominator.bookify.model.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookSummaryDTO {
    private String id;
    private String title;
    private List<String> authors;
    private double price;
    private int stock;
    private String condition;
    private double averageRating;
    private int ratingCount;
    private int totalRating;
    private List<Image> images;
}

