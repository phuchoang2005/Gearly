package com.dominator.gearly.service.admin;

import com.dominator.gearly.dto.AdminProductDTO;
import com.dominator.gearly.dto.ProductCreateDTO;
import com.dominator.gearly.dto.ProductUpdateDTO;
import com.dominator.gearly.mapper.ProductMapper;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Category;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import com.dominator.gearly.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public List<AdminProductDTO> getAllProducts(String titleLike) {
        List<Product> products;
        if (titleLike != null && !titleLike.isBlank()) {
            products = productRepository.findByTitleContainingIgnoreCase(titleLike);
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(product -> productMapper.toAdminDto(product, fetchCategoryNames(product.getCategoryIds())))
                .collect(Collectors.toList());
    }

    public AdminProductDTO getProductById(String id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return null;
        }
        return productMapper.toAdminDto(product, fetchCategoryNames(product.getCategoryIds()));
    }

    public AdminProductDTO createProduct(ProductCreateDTO dto) {
        Product product = productMapper.toEntity(dto);

        // Initialize rating fields
        product.setAverageRating(0);
        product.setRatingCount(0);
        product.setTotalRating(0);

        // set timestamps
        String now = Instant.now().toString();
        product.setAddedAt(now);
        product.setModifiedAt(now);

        Product saved = productRepository.save(product);
        return productMapper.toAdminDto(saved, fetchCategoryNames(saved.getCategoryIds()));
    }

    public AdminProductDTO updateProduct(String id, ProductUpdateDTO dto) {
        Product product = productRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Product not found")
        );

        productMapper.updateEntity(product, dto);
        product.setModifiedAt(Instant.now().toString());

        Product saved = productRepository.save(product);
        return productMapper.toAdminDto(saved, fetchCategoryNames(saved.getCategoryIds()));
    }

    public boolean deleteProduct(String id) {
        try {
            productRepository.deleteById(id);
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
