package com.dominator.bookify.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchDTO {
    private String condition;
    private double minPrice = 0.0;
    private double maxPrice = 200.0;
    private List<String> genres;
    private String sortBy = "title-az";
    private String search;
    private double minRating = 0.0;
    private int page = 0;
    private int size = 12;
}
