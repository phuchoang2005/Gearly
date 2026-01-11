package com.dominator.bookify.controller.admin;

import com.dominator.bookify.dto.*;
import com.dominator.bookify.model.Book;
import com.dominator.bookify.service.admin.AdminBookService;
import com.dominator.bookify.service.user.BookService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin/books")
public class AdminBookController {
    private final BookService bookService;
    private final AdminBookService adminBookService;

    @GetMapping
    public ResponseEntity<?> getAllBooks(
            @RequestParam(value = "title_like", required = false) String titleLike
    ) {
        try {
            List<AdminBookDTO> books = adminBookService.getAllBooks(titleLike);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBook(@PathVariable String id) {
        try {
            AdminBookDTO dto = adminBookService.getBookById(id);
            if (dto == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Book not found"));
            }
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createBook(@RequestBody BookCreateDTO dto) {
        try {
            AdminBookDTO created = adminBookService.createBook(dto);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(created.getId())
                    .toUri();
            return ResponseEntity.created(location).body(created);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(
            @PathVariable String id,
            @RequestBody BookUpdateDTO dto
    ) {
        try {
            AdminBookDTO updated = adminBookService.updateBook(id, dto);
            return ResponseEntity.ok(updated);
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }

    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePatchBook(
            @PathVariable String id,
            @RequestBody BookUpdateDTO dto
    ) {
        try {
            AdminBookDTO updated = adminBookService.updateBook(id, dto);
            return ResponseEntity.ok(updated);
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable String id) {
        try {
            boolean deleted = adminBookService.deleteBook(id);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of("error", "Book not found"));
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
