package com.dominator.bookify.repository.custom;

import com.dominator.bookify.dto.BookSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookRepositoryCustom {
    Page<BookSummaryDTO> findBooks(String condition, double minPrice, double maxPrice,
                                   List<String> genres, String search, double minRating, Pageable pageable);
}
