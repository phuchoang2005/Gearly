package com.dominator.bookify.service.admin;

import com.dominator.bookify.dto.AdminBookDTO;
import com.dominator.bookify.dto.BookCreateDTO;
import com.dominator.bookify.dto.BookUpdateDTO;
import com.dominator.bookify.mapper.BookMapper;
import com.dominator.bookify.model.Book;
import com.dominator.bookify.model.Category;
import com.dominator.bookify.repository.BookRepository;
import com.dominator.bookify.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import com.dominator.bookify.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminBookService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookMapper bookMapper;

    public List<AdminBookDTO> getAllBooks(String titleLike) {
        List<Book> books;
        if (titleLike != null && !titleLike.isBlank()) {
            books = bookRepository.findByTitleContainingIgnoreCase(titleLike);
        } else {
            books = bookRepository.findAll();
        }

        return books.stream()
                .map(book -> bookMapper.toAdminDto(book, fetchCategoryNames(book.getCategoryIds())))
                .collect(Collectors.toList());
    }

    public AdminBookDTO getBookById(String id) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return null;
        }
        return bookMapper.toAdminDto(book, fetchCategoryNames(book.getCategoryIds()));
    }

    public AdminBookDTO createBook(BookCreateDTO dto) {
        Book book = bookMapper.toEntity(dto);

        // Initialize rating fields
        book.setAverageRating(0);
        book.setRatingCount(0);
        book.setTotalRating(0);

        // set timestamps
        String now = Instant.now().toString();
        book.setAddedAt(now);
        book.setModifiedAt(now);

        Book saved = bookRepository.save(book);
        return bookMapper.toAdminDto(saved, fetchCategoryNames(saved.getCategoryIds()));
    }

    public AdminBookDTO updateBook(String id, BookUpdateDTO dto) {
        Book book = bookRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Book not found")
        );

        bookMapper.updateEntity(book, dto);
        book.setModifiedAt(Instant.now().toString());

        Book saved = bookRepository.save(book);
        return bookMapper.toAdminDto(saved, fetchCategoryNames(saved.getCategoryIds()));
    }

    public boolean deleteBook(String id) {
        try {
            bookRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> fetchCategoryNames(List<ObjectId> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        List<String> stringIds = categoryIds.stream()
                .map(ObjectId::toHexString)
                .collect(Collectors.toList());

        List<Category> categories = categoryRepository.findAllById(stringIds);
        return categories.stream().map(Category::getName).collect(Collectors.toList());
    }
}
