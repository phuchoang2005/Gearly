package com.dominator.gearly.controller.admin;

import com.dominator.gearly.dto.*;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.service.admin.AdminProductService;
import com.dominator.gearly.service.user.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService productService;
    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<List<AdminProductDTO>> getAllProducts(
            @RequestParam(value = "title_like", required = false) String titleLike
    ) {
        return ResponseEntity.ok(adminProductService.getAllProducts(titleLike));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProductDTO> getProduct(@PathVariable String id) {
        AdminProductDTO dto = adminProductService.getProductById(id);
        if (dto == null) {
            throw new ResourceNotFoundException("Product not found");
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<AdminProductDTO> createProduct(@RequestBody @Valid ProductCreateDTO dto) {
        AdminProductDTO created = adminProductService.createProduct(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminProductDTO> updateProduct(
            @PathVariable String id,
            @RequestBody @Valid ProductUpdateDTO dto
    ) {
        return ResponseEntity.ok(adminProductService.updateProduct(id, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminProductDTO> updatePatchProduct(
            @PathVariable String id,
            @RequestBody ProductUpdateDTO dto
    ) {
        return ResponseEntity.ok(adminProductService.updateProduct(id, dto));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductSummaryDTO>> searchProducts(ProductSearchDTO searchDTO) {
        return ResponseEntity.ok(productService.getProducts(searchDTO));
    }

    @GetMapping("/bestByRating")
    public ResponseEntity<List<ProductSummaryDTO>> getBestProducts() {
        return ResponseEntity.ok(productService.getBestProducts());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        boolean deleted = adminProductService.deleteProduct(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Product not found");
        }
        return ResponseEntity.noContent().build();
    }
}
