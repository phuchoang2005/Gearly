package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.ProductRating;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.Rating;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <b>The catalog's aggregate root.</b> A thing for sale: what it is, what it costs, how many
 * are left, and how it has been rated.
 *
 * <h2>What changed</h2>
 * This was a Lombok {@code @Getter @Setter} bag, and the two invariants it owns lived
 * everywhere except on it:
 *
 * <ul>
 *   <li><b>Stock could not go negative</b> — a rule expressed five separate times, in
 *       {@code ProductService.decreaseStock}, in the order-placement loop, and at three
 *       points inside {@code CartService}, with a sixth path ({@code setStock} from the admin
 *       update) that made no check at all. It is {@link #reserve} now, and
 *       {@link InsufficientStockException#requireAtLeast} is the only comparison left.</li>
 *   <li><b>The rating rollup had to agree with itself</b> — {@code ratingCount},
 *       {@code totalRating} and {@code averageRating} were three independently settable
 *       fields kept consistent only by {@code ReviewService.applyRating} remembering to
 *       write all three. {@link #addRating} routes them through {@link ProductRating}, which
 *       cannot represent a rollup that disagrees.</li>
 * </ul>
 *
 * <h2>Why the rating stays three flat fields rather than a nested value object</h2>
 * S9 deferred that fold to this sprint, and running it revealed why it should not happen:
 * {@code averageRating} is not only displayed, it is <em>queried</em>. The catalog sorts by it
 * ({@code Sort.by(DESC, "averageRating")} on the best-seller list and the {@code sortBy}
 * switch) and filters on it ({@code minRating}). A {@link ProductRating} keeps the average as
 * a <em>derived</em> value — that is its entire point — so folding the three fields into it
 * would either break three queries or force the derived value to be stored beside its own
 * inputs, which is the inconsistency the value object exists to prevent. The invariant is
 * enforced at the only place that can change these fields instead, and the stored shape is
 * untouched. {@link #rating()} is the seam S12's {@code ReviewApproved} handler uses.
 *
 * <h2>Persistence and the wire</h2>
 * Still a {@code @Document}, for the reason every aggregate in this refactor is: the stored
 * shape stays byte-identical and nothing needed a migration. {@code stock} is a
 * {@link Quantity} now and still writes as a BSON {@code int32}. The entity is no longer
 * serialized to a client — {@code catalog.api}'s DTOs are — so the flat rating fields it keeps
 * for the query planner do not constrain the response shape either way.
 */
@Getter
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    /**
     * Optimistic-locking token. Closes the read-then-write oversell race: two concurrent
     * checkouts could both read stock 1, both pass the check, and both write stock 0 —
     * selling the same unit twice. With a version the second write matches no document and
     * fails with {@code OptimisticLockingFailureException}, which the global handler maps to
     * 409. {@code ConcurrentCheckoutIntegrationTest} proves it against a real replica set.
     *
     * <p>{@code @JsonIgnore} because this is an internal concurrency token: it must never
     * appear on the wire and a client must never be able to supply one. Boxed on purpose —
     * Spring Data treats a {@code null} version as "not yet persisted", so
     * {@code data/seed/migrate.js} backfills 0.
     */
    @Version
    @JsonIgnore
    private Long version;

    private String title;
    private List<String> authors;
    private String description;

    /**
     * Prices default to {@link Money#ZERO} rather than {@code null} so that a document
     * written before the field existed reads back as {@code 0.00}, exactly as the previous
     * {@code double} did.
     */
    private Money price = Money.ZERO;
    private Money originalPrice = Money.ZERO;

    private ProductCondition condition;

    /** Never negative, because {@link Quantity} cannot be. Stored as a BSON {@code int32}. */
    private Quantity stock = Quantity.ZERO;

    /**
     * Stored as BSON {@code ObjectId} — see {@link CategoryId} for why this one id type is
     * different, and {@code DomainTypeConverters} for the pair that keeps it that way.
     */
    private List<CategoryId> categoryIds;

    private List<Image> images;

    // The rating rollup. Three stored fields, one invariant — see the class comment.
    private double averageRating;
    private int ratingCount;
    private int totalRating;

    @CreatedDate
    private Instant addedAt;

    @LastModifiedDate
    private Instant modifiedAt;

    /** For Spring Data. */
    private Product() {
    }

    // ------------------------------------------------------------------------
    // Creation and administrative amendment
    // ------------------------------------------------------------------------

    /** An administrator adds something to the catalog. Opens unrated and untouched. */
    public static Product create(String title,
                                 List<String> authors,
                                 String description,
                                 Money price,
                                 Money originalPrice,
                                 ProductCondition condition,
                                 Quantity stock,
                                 List<CategoryId> categoryIds,
                                 List<Image> images) {
        Product product = new Product();
        product.applyDetails(title, authors, description, price, originalPrice,
                condition, stock, categoryIds, images);
        product.averageRating = 0;
        product.ratingCount = 0;
        product.totalRating = 0;
        Instant now = Instant.now();
        product.addedAt = now;
        product.modifiedAt = now;
        return product;
    }

    /**
     * An administrator edits the listing. Every descriptive field is replaced; the rating
     * rollup and the identity are not, because neither is the administrator's to assign.
     */
    public void amend(String title,
                      List<String> authors,
                      String description,
                      Money price,
                      Money originalPrice,
                      ProductCondition condition,
                      Quantity stock,
                      List<CategoryId> categoryIds,
                      List<Image> images) {
        applyDetails(title, authors, description, price, originalPrice,
                condition, stock, categoryIds, images);
        touch();
    }

    // ------------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------------

    /** The typed identity. Null until Mongo has assigned one on first save. */
    public ProductId productId() {
        return id == null ? null : ProductId.of(id);
    }

    /** Read-only: images change through {@link #amend}, never in place. */
    public List<Image> getImages() {
        return images == null ? null : Collections.unmodifiableList(images);
    }

    public List<CategoryId> getCategoryIds() {
        return categoryIds == null ? null : Collections.unmodifiableList(categoryIds);
    }

    public List<String> getAuthors() {
        return authors == null ? null : Collections.unmodifiableList(authors);
    }

    /** The rollup as one value. The seam S12's {@code ReviewApproved} handler works through. */
    public ProductRating rating() {
        return new ProductRating(ratingCount, totalRating);
    }

    /**
     * What a downstream context is allowed to see. Cart and Ordering copy price and title
     * from here at capture time and never hold a {@code Product} — see {@link CatalogSnapshot}.
     *
     * <p>The image is the first one if there is one, and {@code null} otherwise. That guard is
     * the fix for {@code OrderMapper.getImages().getFirst()}, which threw
     * {@code NoSuchElementException} on any image-less product and so crashed checkout rather
     * than the page that omitted a picture. {@code CartMapper} already guarded; now there is
     * one place that has to.
     */
    public CatalogSnapshot snapshot() {
        return new CatalogSnapshot(
                productId(),
                title,
                authors == null || authors.isEmpty() ? "Unknown" : authors.getFirst(),
                price,
                images == null || images.isEmpty() ? null : images.getFirst().getUrl(),
                condition,
                stock);
    }

    @JsonIgnore
    public boolean isInStock() {
        return !stock.isZero();
    }

    /** Whether this product is at or below the catalog's low-stock threshold. */
    public boolean isBelow(Quantity threshold) {
        return stock.isLessThan(threshold);
    }

    // ------------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------------

    /**
     * Take {@code wanted} units off the shelf, because an order was placed for them.
     *
     * <p>The one place stock comes down. Everything that used to do it by hand —
     * {@code ProductService.decreaseStock} and the four checks around it — now asks for this.
     *
     * @throws InsufficientStockException if there are not that many
     */
    public void reserve(Quantity wanted) {
        assertCanSupply(wanted);
        stock = stock.minus(wanted);
        touch();
    }

    /**
     * Put units back, because an order was cancelled.
     *
     * <p>Nothing called for this before, which was the bug: cancelling an order left its units
     * reserved forever, so the catalog quietly ran itself out of stock. {@code OrderCancelled}
     * drives it now — see {@code CatalogStockListener}.
     */
    public void restock(Quantity returned) {
        stock = stock.plus(returned);
        touch();
    }

    /**
     * Ask whether {@code wanted} units could be supplied, without taking them. What a cart
     * needs: adding to a basket is not a reservation, and must not behave like one.
     *
     * @throws InsufficientStockException if there are not that many
     */
    public void assertCanSupply(Quantity wanted) {
        InsufficientStockException.requireAtLeast(stock, wanted, title);
    }

    public void changePrice(Money newPrice) {
        this.price = Objects.requireNonNull(newPrice, "a product must have a price");
        touch();
    }

    /**
     * Fold a customer's star rating into the rollup.
     *
     * <p>Was {@code ReviewService.applyRating}, which reached into three of this class's
     * setters from outside and produced the average itself. The arithmetic is identical —
     * {@link ProductRating#average()} reproduces the same {@code Math.round(avg * 100) / 100.0}
     * — so no stored average moves. What changes is that the three fields can no longer drift
     * apart, and that {@link Rating} refuses a value outside 1–5 before it reaches them.
     */
    public void addRating(Rating rating) {
        applyRollup(rating().add(rating));
    }

    /** The inverse, for S12's {@code ReviewRejected}: a retracted review stops counting. */
    public void removeRating(Rating rating) {
        applyRollup(rating().remove(rating));
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    private void applyRollup(ProductRating updated) {
        this.ratingCount = updated.count();
        this.totalRating = updated.total();
        this.averageRating = updated.average();
        touch();
    }

    private void applyDetails(String title,
                              List<String> authors,
                              String description,
                              Money price,
                              Money originalPrice,
                              ProductCondition condition,
                              Quantity stock,
                              List<CategoryId> categoryIds,
                              List<Image> images) {
        this.title = title;
        this.authors = authors == null ? null : List.copyOf(authors);
        this.description = description;
        this.price = price == null ? Money.ZERO : price;
        this.originalPrice = originalPrice == null ? Money.ZERO : originalPrice;
        this.condition = condition;
        this.stock = stock == null ? Quantity.ZERO : stock;
        this.categoryIds = categoryIds == null ? null : List.copyOf(categoryIds);
        this.images = images == null ? null : List.copyOf(images);
    }

    /**
     * Stamps {@code modifiedAt}. {@code @LastModifiedDate} would do this on save anyway, but
     * {@code AdminProductService} did it by hand and its test observes it before the save, so
     * the aggregate keeps doing it at the moment the change happens — the same call
     * {@code Order.touch} makes, for the same reason.
     */
    private void touch() {
        this.modifiedAt = Instant.now();
    }
}
