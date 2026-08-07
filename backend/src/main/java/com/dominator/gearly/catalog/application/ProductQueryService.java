package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.api.ProductInLowStockDTO;
import com.dominator.gearly.catalog.api.ProductResponseDTO;
import com.dominator.gearly.catalog.api.ProductResponseMapper;
import com.dominator.gearly.catalog.api.ProductSummaryDTO;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import com.dominator.gearly.catalog.domain.ProductPage;
import com.dominator.gearly.catalog.domain.ProductQuery;
import com.dominator.gearly.catalog.domain.LowStockThreshold;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Every way of reading the catalog: one product, a page of them, the best-rated, the ones
 * running out.
 *
 * <p>Was the read half of {@code ProductService}. Two things changed beyond the move.
 *
 * <p><b>A miss is an exception, not a {@code null}.</b> {@code getProductById} returned
 * {@code null}, and two of its own callers dereferenced the result immediately —
 * {@code getStock} and {@code decreaseStock} — so a product delisted between browsing and
 * checking out turned a customer's checkout into a {@code NullPointerException} and an opaque
 * 500. Three other callers had each grown their own {@code if (product == null)} guard with a
 * slightly different message. {@link #require} is the one answer now, and it is a 404.
 *
 * <p><b>The category names are a projection.</b> They used to be a {@code @Transient} field
 * this method populated on the aggregate on the way past — see {@link CategoryNameProjection}.
 */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    /** Alphabetical by title — the order the wishlist has always been shown in. */
    private static final Comparator<Product> BY_TITLE = Comparator.comparing(Product::getTitle);

    private final ProductRepository products;
    private final CategoryNameProjection categoryNames;
    private final ProductResponseMapper mapper;
    private final LowStockThreshold lowStockThreshold;

    /** @throws ProductNotFoundException if there is no such product — a 404, never a null */
    public ProductResponseDTO getProduct(String id) {
        Product product = require(ProductId.of(id));
        return mapper.toResponseDto(product, categoryNames.namesOf(product.getCategoryIds()));
    }

    public Page<ProductSummaryDTO> search(ProductQuery query) {
        ProductPage page = products.search(query);
        return new PageImpl<>(
                page.content().stream().map(mapper::toSummaryDto).toList(),
                PageRequest.of(page.page(), page.size()),
                page.totalElements());
    }

    /**
     * A page drawn from a set of ids the caller already holds — the wishlist, in both of the
     * two shapes it is served in.
     *
     * <p>Filtered and paged in memory because the ids come from the user document rather than
     * from a catalog query, which is how it has always worked. The two callers differ only in
     * whether the result is title-ordered, and that difference is preserved rather than
     * quietly reconciled: {@code GET /api/products} does not sort and {@code GET /api/wishlist}
     * does. S12 owns the wishlist and can decide they should agree.
     */
    public Page<ProductSummaryDTO> getProductsByIds(List<String> ids, String searchTxt,
                                                    int pageIndex, int pageSize, boolean sorted) {
        String keyword = searchTxt == null ? "" : searchTxt.trim().toLowerCase();

        List<Product> matched = products.findAllById(ids.stream().map(ProductId::of).toList()).stream()
                .filter(product -> matches(product, keyword))
                .toList();

        List<ProductSummaryDTO> summaries = (sorted ? matched.stream().sorted(BY_TITLE) : matched.stream())
                .map(mapper::toSummaryDto)
                .toList();

        int start = Math.min(pageIndex * pageSize, summaries.size());
        int end = Math.min(start + pageSize, summaries.size());
        if (sorted && start >= end) {
            return Page.empty();
        }

        PageRequest pageRequest = sorted
                ? PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "title"))
                : PageRequest.of(pageIndex, pageSize);

        return new PageImpl<>(summaries.subList(start, end), pageRequest, summaries.size());
    }

    /** The storefront's "best rated" strip. */
    public List<ProductSummaryDTO> getBestProducts() {
        return products.findTopRated(16).stream().map(mapper::toSummaryDto).toList();
    }

    /**
     * The admin dashboard's low-stock warning. The threshold arrives from
     * {@code gearly.catalog.low-stock-threshold}, where it used to be {@code 10} inside a
     * Spring Data {@code @Query} annotation.
     */
    public List<ProductInLowStockDTO> getLowStockProducts() {
        return products.findLowStock(lowStockThreshold.value()).stream()
                .map(mapper::toLowStockDto)
                .toList();
    }

    /**
     * The aggregate itself, for the paths inside this context that have to change it.
     * Anything <em>outside</em> the catalog goes through {@code ProductSnapshotPort} instead.
     */
    public Product require(ProductId productId) {
        return products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private static boolean matches(Product product, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }
        if (product.getTitle() != null && product.getTitle().toLowerCase().contains(keyword)) {
            return true;
        }
        return product.getAuthors() != null && product.getAuthors().stream()
                .anyMatch(author -> author.toLowerCase().contains(keyword));
    }
}
