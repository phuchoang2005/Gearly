package com.dominator.bookify.repository;

import com.dominator.bookify.dto.BookSummaryDTO;
import com.dominator.bookify.model.Book;
import com.dominator.bookify.repository.custom.BookRepositoryCustom;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends MongoRepository<Book, String>, BookRepositoryCustom {
    List<BookSummaryDTO> findByOrderByAverageRatingDesc(Pageable pageable);
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByCategoryIdsIn(List<ObjectId> categoryIds);

    String title(String title);
}
