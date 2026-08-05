package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.api.AdminProductDTO;
import com.dominator.gearly.catalog.api.ProductCreateDTO;
import com.dominator.gearly.catalog.api.ProductResponseMapper;
import com.dominator.gearly.catalog.api.ProductUpdateDTO;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Administering the catalog: listing, creating, editing and removing products.
 *
 * <p>The write half is what changed. It used to hand a bare entity to
 * {@code ProductMapper.toEntity} / {@code updateEntity}, which assigned nine fields through
 * nine setters, and then set the rating fields and the timestamps itself afterwards — so
 * "create a product" was spread across three classes and depended on each of them remembering
 * its share. {@code Product.create} and {@code Product.amend} are one call each, and the
 * aggregate decides what an administrator may and may not assign: the rating rollup is not on
 * the list, because it belongs to the customers who gave it.
 */
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository products;
    private final CategoryNameProjection categoryNames;
    private final ProductResponseMapper mapper;

    public List<AdminProductDTO> getAllProducts(String titleLike) {
        List<Product> found = titleLike != null && !titleLike.isBlank()
                ? products.findByTitleContaining(titleLike)
                : products.findAll();

        return found.stream().map(this::toAdminDto).toList();
    }

    /** @throws ProductNotFoundException if there is no such product — a 404, never a null */
    public AdminProductDTO getProductById(String id) {
        return toAdminDto(require(id));
    }

    public AdminProductDTO createProduct(ProductCreateDTO dto) {
        Product product = Product.create(
                dto.getTitle(),
                dto.getAuthors(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getOriginalPrice(),
                dto.getCondition(),
                Quantity.of(dto.getStock()),
                dto.getCategoryIds(),
                dto.getImages());

        return toAdminDto(products.save(product));
    }

    public AdminProductDTO updateProduct(String id, ProductUpdateDTO dto) {
        Product product = require(id);
        product.amend(
                dto.getTitle(),
                dto.getAuthors(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getOriginalPrice(),
                dto.getCondition(),
                Quantity.of(dto.getStock()),
                dto.getCategoryIds(),
                dto.getImages());

        return toAdminDto(products.save(product));
    }

    /**
     * @return {@code false} when there was no such product.
     *
     * <p>This used to be a {@code try/catch (Exception)} around {@code deleteById} that
     * reported "not found" for anything that went wrong, including a database that was down.
     * The adapter checks existence instead, so a 404 now means the product is absent rather
     * than that something — anything — failed.
     */
    public boolean deleteProduct(String id) {
        return products.deleteById(ProductId.of(id));
    }

    private Product require(String id) {
        ProductId productId = ProductId.of(id);
        return products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private AdminProductDTO toAdminDto(Product product) {
        return mapper.toAdminDto(product, categoryNames.namesOf(product.getCategoryIds()));
    }
}
