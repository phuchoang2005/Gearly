package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;

import java.util.List;
import java.util.Optional;

/**
 * The catalog's repository port, stated in the domain's own vocabulary: typed ids in,
 * aggregates out. The MongoDB adapter behind it is {@code MongoProductRepository}.
 *
 * <p>Replaces three separate Spring Data interfaces that had grown apart —
 * {@code ProductRepository}, {@code ProductsInStockRepository} (one method, whose
 * {@code @Query("{'stock': {$lt: 10}}")} put a business rule inside an annotation) and
 * {@code ProductRepositoryCustom} with its {@code MongoTemplate} implementation. The rule that
 * was in the annotation is a parameter of {@link #findLowStock} now, bound from configuration.
 *
 * <p>Nothing here returns a DTO. The old custom implementation projected search results
 * straight into {@code ProductSummaryDTO}, which put a response shape into the persistence
 * layer and meant the query could not be reused by anything that wanted a different view. It
 * reads whole documents either way, so mapping in the application layer costs nothing.
 */
public interface ProductRepository {

    Optional<Product> findById(ProductId id);

    /** Missing ids are absent from the result rather than an error. */
    List<Product> findAllById(List<ProductId> ids);

    List<Product> findAll();

    Product save(Product product);

    List<Product> saveAll(List<Product> products);

    /** @return whether a product with that id existed to delete */
    boolean deleteById(ProductId id);

    List<Product> findByTitleContaining(String title);

    List<Product> findByAnyCategory(List<CategoryId> categoryIds);

    /** Everything with fewer than {@code threshold} units left. */
    List<Product> findLowStock(Quantity threshold);

    /** The best-rated products, highest average first. */
    List<Product> findTopRated(int limit);

    /** The storefront's faceted search. */
    ProductPage search(ProductQuery query);
}
