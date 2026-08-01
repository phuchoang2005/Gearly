package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.BookSearchDTO;
import com.dominator.gearly.dto.BookSummaryDTO;
import com.dominator.gearly.dto.WishlistRequestDTO;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.model.Book;
import com.dominator.gearly.service.user.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable String id) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found");
        }
        return ResponseEntity.ok(book);
    }

    @GetMapping()
    public ResponseEntity<Page<BookSummaryDTO>> getBooksByIds(WishlistRequestDTO dto) {
        return ResponseEntity.ok(bookService.getBooksByIds(dto));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookSummaryDTO>> searchBooks(BookSearchDTO searchDTO) {
        return ResponseEntity.ok(bookService.getBooks(searchDTO));
    }

    @GetMapping("/bestByRating")
    public ResponseEntity<List<BookSummaryDTO>> getBestBooks() {
        return ResponseEntity.ok(bookService.getBestBooks());
    }
}
