package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.CategoryProductCountDTO;
import com.dominator.gearly.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryProductCountDTO> getCategoriesWithProductCount() {
        return categoryRepository.findCategoryWithProductCount();
    }
}
