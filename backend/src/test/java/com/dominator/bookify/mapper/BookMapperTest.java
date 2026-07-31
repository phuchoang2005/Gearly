package com.dominator.bookify.mapper;

import com.dominator.bookify.dto.AdminBookDTO;
import com.dominator.bookify.dto.BookCreateDTO;
import com.dominator.bookify.dto.BookInLowStockDTO;
import com.dominator.bookify.dto.BookSummaryDTO;
import com.dominator.bookify.dto.BookUpdateDTO;
import com.dominator.bookify.model.Book;
import com.dominator.bookify.model.Image;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookMapperTest {

    private final BookMapper mapper = new BookMapper();

    @Test
    void toEntity_copiesFields_andRewrapsImages() {
        ObjectId cat = new ObjectId();
        Image img = new Image("http://img/1.png", "gpu");

        BookCreateDTO dto = new BookCreateDTO();
        dto.setTitle("RTX 4090");
        dto.setAuthors(List.of("NVIDIA"));
        dto.setDescription("GPU");
        dto.setPrice(1599.0);
        dto.setOriginalPrice(1799.0);
        dto.setCondition("NEW");
        dto.setStock(5);
        dto.setCategoryIds(List.of(cat));
        dto.setImages(List.of(img));

        Book book = mapper.toEntity(dto);

        assertThat(book.getTitle()).isEqualTo("RTX 4090");
        assertThat(book.getAuthors()).containsExactly("NVIDIA");
        assertThat(book.getPrice()).isEqualTo(1599.0);
        assertThat(book.getOriginalPrice()).isEqualTo(1799.0);
        assertThat(book.getStock()).isEqualTo(5);
        assertThat(book.getCategoryIds()).containsExactly(cat);
        assertThat(book.getImages()).hasSize(1);
        assertThat(book.getImages().get(0)).isNotSameAs(img); // defensively re-wrapped
        assertThat(book.getImages().get(0).getUrl()).isEqualTo("http://img/1.png");
        assertThat(book.getImages().get(0).getAlt()).isEqualTo("gpu");
    }

    @Test
    void updateEntity_updatesFields_preservesIdAndRatings_convertsCategoryIds() {
        Book book = new Book();
        book.setId("b1");
        book.setAverageRating(4.5);
        book.setRatingCount(10);
        book.setTotalRating(45);
        book.setAddedAt("2025-01-01T00:00:00Z");

        String hex = new ObjectId().toHexString();
        BookUpdateDTO dto = new BookUpdateDTO();
        dto.setTitle("New Title");
        dto.setAuthors(List.of("A"));
        dto.setDescription("d");
        dto.setPrice(10.0);
        dto.setOriginalPrice(12.0);
        dto.setCondition("USED");
        dto.setStock(3);
        dto.setImages(List.of(new Image("u", "a")));
        dto.setCategoryIds(List.of(hex));

        mapper.updateEntity(book, dto);

        // preserved
        assertThat(book.getId()).isEqualTo("b1");
        assertThat(book.getAverageRating()).isEqualTo(4.5);
        assertThat(book.getRatingCount()).isEqualTo(10);
        assertThat(book.getTotalRating()).isEqualTo(45);
        assertThat(book.getAddedAt()).isEqualTo("2025-01-01T00:00:00Z");
        // updated
        assertThat(book.getTitle()).isEqualTo("New Title");
        assertThat(book.getCondition()).isEqualTo("USED");
        assertThat(book.getStock()).isEqualTo(3);
        // hex string ids are stored as ObjectId, consistent with create/read
        assertThat(book.getCategoryIds()).containsExactly(new ObjectId(hex));
    }

    @Test
    void summaryAdminAndLowStock_dtos() {
        ObjectId cat = new ObjectId();
        Book book = new Book();
        book.setId("b1");
        book.setTitle("Case");
        book.setAuthors(List.of("Brand"));
        book.setPrice(80.0);
        book.setStock(7);
        book.setCondition("NEW");
        book.setAverageRating(4.0);
        book.setRatingCount(2);
        book.setTotalRating(8);
        book.setImages(List.of(new Image("u", "a")));
        book.setCategoryIds(List.of(cat));

        BookSummaryDTO summary = mapper.toSummaryDto(book);
        assertThat(summary.getId()).isEqualTo("b1");
        assertThat(summary.getTitle()).isEqualTo("Case");
        assertThat(summary.getPrice()).isEqualTo(80.0);
        assertThat(summary.getImages()).hasSize(1);

        AdminBookDTO admin = mapper.toAdminDto(book, List.of("Cases"));
        assertThat(admin.getId()).isEqualTo("b1");
        assertThat(admin.getCategoryIds()).containsExactly(cat);
        assertThat(admin.getCategoryNames()).containsExactly("Cases");
        assertThat(admin.getTotalRating()).isEqualTo(8);

        BookInLowStockDTO low = mapper.toLowStockDto(book);
        assertThat(low.getId()).isEqualTo("b1");
        assertThat(low.getTitle()).isEqualTo("Case");
        assertThat(low.getStock()).isEqualTo(7);
    }
}
