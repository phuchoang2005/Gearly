package com.dominator.bookify.dto;

import lombok.Data;

import java.util.List;

@Data
public class TopCategoryQuantityDTO {
    private String categoryId;
    private String categoryName;
    private List<BookQuantityDTO> top10Books;

    @Data
    public static class BookQuantityDTO {
        private String bookId;
        private String title;
        private int totalQuantitySold;
    }
}
