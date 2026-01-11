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
public class BookUpdateDTO {
    private String title;
    private List<String> authors;
    private String description;
    private double price;
    private double originalPrice;
    private String condition;
    private int stock;
    private List<String> categoryIds;
    private List<Image> images;
}

