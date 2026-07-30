package com.dominator.bookify.controller.admin;

import com.dominator.bookify.dto.CategoryBookCountDTO;
import com.dominator.bookify.service.user.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryBookCountDTO>> findAll() {
        return ResponseEntity.ok(categoryService.getCategoriesWithBookCount());
    }
}
