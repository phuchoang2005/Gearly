package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.BookSearchDTO;
import com.dominator.bookify.dto.BookSummaryDTO;
import com.dominator.bookify.dto.WishlistRequestDTO;
import com.dominator.bookify.model.Book;
import com.dominator.bookify.service.user.BookService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getBook(@PathVariable String id) {
        try {
            Book book = bookService.getBookById(id);
            if (book == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Bok not foound"));
            }
            return ResponseEntity.ok(book);
        } catch(Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping()
    public ResponseEntity<?> getBooksByIds(WishlistRequestDTO dto) {
        try {
            Page<BookSummaryDTO> books = bookService.getBooksByIds(dto);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/search")
    public ResponseEntity<?> searchBooks(BookSearchDTO searchDTO) {
        try {
            Page<BookSummaryDTO> books = bookService.getBooks(searchDTO);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bestByRating")
    public ResponseEntity<?> getBestBooks() {
        try {
            List<BookSummaryDTO> bestBooks = bookService.getBestBooks();
            return ResponseEntity.ok(bestBooks);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
