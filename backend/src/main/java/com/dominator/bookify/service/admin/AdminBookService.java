// src/main/java/com/dominator/bookify/service/AdminBookService.java
package com.dominator.bookify.service.admin;

import com.dominator.bookify.dto.AdminBookDTO;
import com.dominator.bookify.dto.BookCreateDTO;
import com.dominator.bookify.dto.BookUpdateDTO;

import java.util.List;

public interface AdminBookService {
    List<AdminBookDTO> getAllBooks(String titleLike);
    AdminBookDTO getBookById(String id);
    AdminBookDTO createBook(BookCreateDTO dto);
    AdminBookDTO updateBook(String id, BookUpdateDTO dto);
    boolean deleteBook(String id);
}
