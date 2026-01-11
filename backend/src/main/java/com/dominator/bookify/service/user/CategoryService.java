package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.CategoryBookCountDTO;
import com.dominator.bookify.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryBookCountDTO> getCategoriesWithBookCount() {
        return categoryRepository.findCategoryWithBookCount();
    }
}
