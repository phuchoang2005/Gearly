package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.application.AdminProductService;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.catalog.application.ProductQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * The admin console's catalog management. Same routes, same shapes.
 *
 * <p><b>Defence in depth.</b> {@code @PreAuthorize("hasRole('ADMIN')")} repeats the
 * {@code /api/admin/**} URL rule from {@code SecurityConfig} at the class level rather than
 * replacing it, for the reason {@code AdminUserController} spells out: the URL rule is a prefix
 * match on a string, and every endpoint that has ever escaped one did so by being mounted
 * somewhere the pattern did not reach. The annotation travels with the code.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductQueryService productQueryService;
    private final AdminProductService adminProductService;
    private final ProductSearchMapper searchMapper;

    @GetMapping
    public ResponseEntity<List<AdminProductDTO>> getAllProducts(
            @RequestParam(value = "title_like", required = false) String titleLike
    ) {
        return ResponseEntity.ok(adminProductService.getAllProducts(titleLike));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProductDTO> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(adminProductService.getProductById(id));
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
        return ResponseEntity.ok(productQueryService.search(searchMapper.toQuery(searchDTO)));
    }

    @GetMapping("/bestByRating")
    public ResponseEntity<List<ProductSummaryDTO>> getBestProducts() {
        return ResponseEntity.ok(productQueryService.getBestProducts());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (!adminProductService.deleteProduct(id)) {
            throw new ProductNotFoundException(ProductId.of(id));
        }
        return ResponseEntity.noContent().build();
    }
}
