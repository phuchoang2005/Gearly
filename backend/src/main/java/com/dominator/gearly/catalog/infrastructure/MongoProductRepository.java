package com.dominator.gearly.catalog.infrastructure;

import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductPage;
import com.dominator.gearly.catalog.domain.ProductQuery;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.catalog.domain.ProductSort;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The MongoDB adapter behind {@link ProductRepository}. The only class in the catalog that
 * knows a product is stored in MongoDB, that a category id is an {@code ObjectId} on the way
 * in, or that "low stock" is a {@code $lt} on a field called {@code stock}.
 *
 * <p>The faceted search builds criteria directly, which is what {@code ProductRepositoryCustomImpl}
 * did before it moved in here. It is exempt from the {@code MongoTemplate} rule for the reason
 * S10 established when it narrowed that rule: a repository adapter is by definition the layer
 * that knows the storage technology, and a dozen optional clauses is not something the
 * derived-query DSL can build.
 */
@Repository
@RequiredArgsConstructor
public class MongoProductRepository implements ProductRepository {

    private final SpringDataProductRepository products;
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Product> findById(ProductId id) {
        return products.findById(id.value());
    }

    @Override
    public List<Product> findAllById(List<ProductId> ids) {
        return products.findAllById(ids.stream().map(ProductId::value).toList());
    }

    @Override
    public List<Product> findAll() {
        return products.findAll();
    }

    @Override
    public Product save(Product product) {
        return products.save(product);
    }

    @Override
    public List<Product> saveAll(List<Product> toSave) {
        return products.saveAll(toSave);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The existence check is the difference from {@code AdminProductService.deleteProduct},
     * which wrapped {@code deleteById} in a {@code catch (Exception)} and reported "not found"
     * for anything that went wrong — including a database that was down. Mongo's
     * {@code deleteById} is silent on a missing id, so asking first is the only way to tell
     * the two apart.
     */
    @Override
    public boolean deleteById(ProductId id) {
        if (!products.existsById(id.value())) {
            return false;
        }
        products.deleteById(id.value());
        return true;
    }

    @Override
    public List<Product> findByTitleContaining(String title) {
        return products.findByTitleContainingIgnoreCase(title);
    }

    /** The threshold arrives as a parameter, where it used to be {@code 10} inside a {@code @Query}. */
    @Override
    public List<Product> findLowStock(Quantity threshold) {
        return mongoTemplate.find(
                Query.query(Criteria.where("stock").lt(threshold.toInt())), Product.class);
    }

    @Override
    public List<Product> findTopRated(int limit) {
        return mongoTemplate.find(
                new Query().with(Sort.by(Sort.Direction.DESC, "averageRating")).limit(limit),
                Product.class);
    }

    @Override
    public ProductPage search(ProductQuery productQuery) {
        Query query = new Query();

        // Matched on the enum's stored token rather than on a caller-supplied string, so a
        // value the catalog does not use can no longer reach the query and silently match
        // nothing — the api layer rejects it at the edge instead.
        if (productQuery.hasCondition()) {
            query.addCriteria(Criteria.where("condition").is(productQuery.condition().wireValue()));
        }

        query.addCriteria(Criteria.where("price")
                .gte(productQuery.minPrice()).lte(productQuery.maxPrice()));

        if (productQuery.hasCategories()) {
            List<ObjectId> objectIds = productQuery.categoryIds().stream()
                    .map(CategoryId::value)
                    .map(ObjectId::new)
                    .toList();
            query.addCriteria(Criteria.where("categoryIds").in(objectIds));
        }

        if (productQuery.hasSearchTerm()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(productQuery.searchTerm(), "i"),
                    Criteria.where("authors").regex(productQuery.searchTerm(), "i")));
        }

        if (productQuery.hasMinRating()) {
            query.addCriteria(Criteria.where("averageRating").gte(productQuery.minRating()));
        }

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Product.class);

        List<Product> content = mongoTemplate.find(
                query.with(sortOf(productQuery.sort()))
                        .skip((long) productQuery.page() * productQuery.size())
                        .limit(productQuery.size()),
                Product.class);

        return new ProductPage(content, productQuery.page(), productQuery.size(), total);
    }

    private static Sort sortOf(ProductSort sort) {
        return Sort.by(sort.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC, sort.field());
    }
}
