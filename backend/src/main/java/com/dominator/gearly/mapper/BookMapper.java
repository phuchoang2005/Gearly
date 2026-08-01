package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.AdminBookDTO;
import com.dominator.gearly.dto.BookCreateDTO;
import com.dominator.gearly.dto.BookInLowStockDTO;
import com.dominator.gearly.dto.BookSummaryDTO;
import com.dominator.gearly.dto.BookUpdateDTO;
import com.dominator.gearly.model.Book;
import com.dominator.gearly.model.Image;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@link Book} entities to catalog/admin DTOs and builds entities from
 * create/update requests. Category-name resolution stays in the service layer;
 * callers pass the resolved names into {@link #toAdminDto(Book, List)}.
 */
@Component
public class BookMapper {

    /** Compact catalog card used by search, wishlist, and best-seller listings. */
    public BookSummaryDTO toSummaryDto(Book book) {
        return new BookSummaryDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthors(),
                book.getPrice(),
                book.getStock(),
                book.getCondition(),
                book.getAverageRating(),
                book.getRatingCount(),
                book.getTotalRating(),
                book.getImages()
        );
    }

    /** Full admin view; {@code categoryNames} is resolved by the caller. */
    public AdminBookDTO toAdminDto(Book book, List<String> categoryNames) {
        AdminBookDTO dto = new AdminBookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthors(book.getAuthors());
        dto.setDescription(book.getDescription());
        dto.setPrice(book.getPrice());
        dto.setOriginalPrice(book.getOriginalPrice());
        dto.setCondition(book.getCondition());
        dto.setStock(book.getStock());
        dto.setCategoryIds(book.getCategoryIds());
        dto.setCategoryNames(categoryNames);
        dto.setImages(book.getImages());
        dto.setAverageRating(book.getAverageRating());
        dto.setRatingCount(book.getRatingCount());
        dto.setTotalRating(book.getTotalRating());
        return dto;
    }

    public BookInLowStockDTO toLowStockDto(Book book) {
        return new BookInLowStockDTO(book.getId(), book.getTitle(), book.getStock());
    }

    /** Builds a new entity from a create request. Rating fields and timestamps stay with the service. */
    public Book toEntity(BookCreateDTO dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthors(dto.getAuthors());
        book.setDescription(dto.getDescription());
        book.setPrice(dto.getPrice());
        book.setOriginalPrice(dto.getOriginalPrice());
        book.setCondition(dto.getCondition());
        book.setStock(dto.getStock());
        book.setCategoryIds(dto.getCategoryIds());
        book.setImages(copyImages(dto.getImages()));
        return book;
    }

    /** Applies an update request onto an existing entity, preserving id, ratings, and timestamps. */
    public void updateEntity(Book book, BookUpdateDTO dto) {
        book.setTitle(dto.getTitle());
        book.setAuthors(dto.getAuthors());
        book.setDescription(dto.getDescription());
        book.setPrice(dto.getPrice());
        book.setOriginalPrice(dto.getOriginalPrice());
        book.setCondition(dto.getCondition());
        book.setStock(dto.getStock());
        book.setImages(dto.getImages());
        // The update DTO carries category ids as hex strings; store them as ObjectId,
        // consistent with create and the read path (Book.categoryIds is List<ObjectId>).
        book.setCategoryIds(toObjectIds(dto.getCategoryIds()));
    }

    private List<Image> copyImages(List<Image> images) {
        if (images == null) {
            return null;
        }
        return images.stream()
                .map(i -> new Image(i.getUrl(), i.getAlt()))
                .collect(Collectors.toList());
    }

    private List<ObjectId> toObjectIds(List<String> ids) {
        if (ids == null) {
            return null;
        }
        return ids.stream().map(ObjectId::new).collect(Collectors.toList());
    }
}
