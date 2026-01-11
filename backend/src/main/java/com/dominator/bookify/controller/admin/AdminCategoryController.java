package com.dominator.bookify.controller.admin;

import com.dominator.bookify.dto.CategoryBookCountDTO;
import com.dominator.bookify.service.user.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            List<CategoryBookCountDTO> categories = categoryService.getCategoriesWithBookCount();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
