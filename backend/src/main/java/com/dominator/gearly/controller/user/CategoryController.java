package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.CategoryBookCountDTO;
import com.dominator.gearly.service.user.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryBookCountDTO>> findAll() {
        return ResponseEntity.ok(categoryService.getCategoriesWithBookCount());
    }
}
