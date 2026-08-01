package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.ProductSearchDTO;
import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.dto.WishlistRequestDTO;
import com.dominator.gearly.mapper.ProductMapper;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Category;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.*;
import com.dominator.gearly.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public Product getProductById(String id) {
        return productRepository.findById(id).map(product -> {
            List<ObjectId> categoryIds = product.getCategoryIds();
            if (categoryIds != null && !categoryIds.isEmpty()) {
                List<String> categoryIdStrings = categoryIds.stream()
                        .map(ObjectId::toHexString)
                        .toList();

                List<String> names = categoryRepository.findAllById(categoryIdStrings)
                        .stream()
                        .map(Category::getName)
                        .toList();

                product.setCategoryNames(names);
            }
            return product;
        }).orElse(null);
    }


    public int getStock(String productId) {
        Product product = getProductById(productId);
        return product.getStock();
    }

    public void decreaseStock(String productId, int quantity) {
        Product product = getProductById(productId);
        if (product.getStock() < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + product.getTitle());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    public Page<ProductSummaryDTO> getProductsByIds(WishlistRequestDTO dto) {
        List<Product> products = productRepository.findAllById(dto.getIds());

        String searchLower = dto.getSearchTxt().toLowerCase().trim();
        List<Product> filtered = products.stream()
                .filter(product ->
                        product.getTitle().toLowerCase().contains(searchLower) ||
                                (product.getAuthors() != null && product.getAuthors().stream()
                                        .anyMatch(author -> author.toLowerCase().contains(searchLower)))
                )
                .toList();

        List<ProductSummaryDTO> summaries = filtered.stream()
                .map(productMapper::toSummaryDto)
                .collect(Collectors.toList());

        int start = Math.min(dto.getPageIndex() * dto.getPageSize(), summaries.size());
        int end = Math.min(start + dto.getPageSize(), summaries.size());

        return new PageImpl<>(
                summaries.subList(start, end),
                PageRequest.of(dto.getPageIndex(), dto.getPageSize()),
                summaries.size()
        );
    }


    public Page<ProductSummaryDTO> getProducts(ProductSearchDTO searchDTO) {
        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), getSort(searchDTO.getSortBy()));
        return productRepository.findProducts(
                searchDTO.getCondition(),
                searchDTO.getMinPrice(),
                searchDTO.getMaxPrice(),
                searchDTO.getGenres(),
                searchDTO.getSearch(),
                searchDTO.getMinRating(),
                pageable
        );
    }

    private Sort getSort(String sortBy) {
        return switch (sortBy) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "addedAt");
            case "price-low" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-high" -> Sort.by(Sort.Direction.DESC, "price");
            case "title-az" -> Sort.by(Sort.Direction.ASC, "title");
            case "title-za" -> Sort.by(Sort.Direction.DESC, "title");
            default -> Sort.by(Sort.Direction.ASC, "title");
        };
    }

    public List<ProductSummaryDTO> getBestProducts() {
        Pageable pageable = PageRequest.of(0, 16, Sort.by(Sort.Direction.DESC, "averageRating"));
        return productRepository.findByOrderByAverageRatingDesc(pageable);
    }
    public List<Product> getProductsByTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }

        // Basic cleanup
        String keyword = userMessage.trim();

        // MongoDB search (case-insensitive, partial match)
        List<Product> results = productRepository.findByTitleContainingIgnoreCase(keyword);

        // Safety: limit results for AI usage
        return results.stream()
                .limit(5)
                .toList();
    }
    
    public List<Product> getProductsByCategoryName(String categoryName) {
        // 1. Find the category ID(s) matching the name
        List<Category> categories = categoryRepository.findByNameContainingIgnoreCase(categoryName);
        
        if (categories.isEmpty()) return List.of();

        // 2. Get IDs
        List<ObjectId> categoryIds = categories.stream()
                .map(Category::getId)
                .map(ObjectId::new) // Ensure correct type for MongoDB
                .toList();

        // 3. Find products with those Category IDs
        // (You might need to add this method to ProductRepository too: findByCategoryIdsIn(List<ObjectId> ids))
        return productRepository.findByCategoryIdsIn(categoryIds).stream().limit(5).toList();
    }
}
