package com.dominator.bookify.repository;
import com.dominator.bookify.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface BooksInStockRepository extends MongoRepository<Book,String> {
    @Query("{'stock' : { $lt : 10 }}")
    List<Book> findBooksWithLowStock();
}
