package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.ProductSearchDTO;
import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.dto.WishlistRequestDTO;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.service.user.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found");
        }
        return ResponseEntity.ok(product);
    }

    @GetMapping()
    public ResponseEntity<Page<ProductSummaryDTO>> getProductsByIds(WishlistRequestDTO dto) {
        return ResponseEntity.ok(productService.getProductsByIds(dto));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductSummaryDTO>> searchProducts(ProductSearchDTO searchDTO) {
        return ResponseEntity.ok(productService.getProducts(searchDTO));
    }

    @GetMapping("/bestByRating")
    public ResponseEntity<List<ProductSummaryDTO>> getBestProducts() {
        return ResponseEntity.ok(productService.getBestProducts());
    }
}
