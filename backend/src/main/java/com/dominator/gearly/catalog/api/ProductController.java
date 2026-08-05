package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.application.ProductQueryService;
import com.dominator.gearly.dto.WishlistRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The storefront's read-only view of the catalog. Same four routes, same four response shapes.
 *
 * <p>The {@code null} check that used to sit in {@code getProduct} is gone: the use case
 * throws {@code ProductNotFoundException} and {@code GlobalExceptionHandler} answers 404, so
 * the controller no longer has to know what a miss looks like.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductSearchMapper searchMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productQueryService.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProductSummaryDTO>> getProductsByIds(WishlistRequestDTO dto) {
        return ResponseEntity.ok(productQueryService.getProductsByIds(
                dto.getIds(), dto.getSearchTxt(), dto.getPageIndex(), dto.getPageSize(), false));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductSummaryDTO>> searchProducts(ProductSearchDTO searchDTO) {
        return ResponseEntity.ok(productQueryService.search(searchMapper.toQuery(searchDTO)));
    }

    @GetMapping("/bestByRating")
    public ResponseEntity<List<ProductSummaryDTO>> getBestProducts() {
        return ResponseEntity.ok(productQueryService.getBestProducts());
    }
}
