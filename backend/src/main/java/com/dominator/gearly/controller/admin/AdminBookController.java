package com.dominator.gearly.controller.admin;

import com.dominator.gearly.dto.*;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.service.admin.AdminBookService;
import com.dominator.gearly.service.user.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/books")
public class AdminBookController {
    private final BookService bookService;
    private final AdminBookService adminBookService;

    @GetMapping
    public ResponseEntity<List<AdminBookDTO>> getAllBooks(
            @RequestParam(value = "title_like", required = false) String titleLike
    ) {
        return ResponseEntity.ok(adminBookService.getAllBooks(titleLike));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminBookDTO> getBook(@PathVariable String id) {
        AdminBookDTO dto = adminBookService.getBookById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("Book not found");
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<AdminBookDTO> createBook(@RequestBody @Valid BookCreateDTO dto) {
        AdminBookDTO created = adminBookService.createBook(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminBookDTO> updateBook(
            @PathVariable String id,
            @RequestBody @Valid BookUpdateDTO dto
    ) {
        return ResponseEntity.ok(adminBookService.updateBook(id, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminBookDTO> updatePatchBook(
            @PathVariable String id,
            @RequestBody BookUpdateDTO dto
    ) {
        return ResponseEntity.ok(adminBookService.updateBook(id, dto));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookSummaryDTO>> searchBooks(BookSearchDTO searchDTO) {
        return ResponseEntity.ok(bookService.getBooks(searchDTO));
    }

    @GetMapping("/bestByRating")
    public ResponseEntity<List<BookSummaryDTO>> getBestBooks() {
        return ResponseEntity.ok(bookService.getBestBooks());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable String id) {
        boolean deleted = adminBookService.deleteBook(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Book not found");
        }
        return ResponseEntity.noContent().build();
    }
}
