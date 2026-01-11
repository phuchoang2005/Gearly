package com.dominator.bookify.dto;

import com.dominator.bookify.model.Image;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookDTO {
    private String id;
    private String title;
    private List<String> authors;
    private String description;
    private double price;
    private double originalPrice;
    private String condition;
    private int stock;
    private List<ObjectId> categoryIds;
    private List<String> categoryNames;
    private List<Image> images;
    private double averageRating;
    private int ratingCount;
    private int totalRating;
}

