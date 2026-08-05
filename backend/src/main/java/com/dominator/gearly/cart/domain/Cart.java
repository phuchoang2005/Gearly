package com.dominator.gearly.cart.domain;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>The cart aggregate.</b> A customer's basket before checkout, whether they are signed in
 * or not.
 *
 * <h2>What changed</h2>
 * {@code CartService} was 237 lines and held every rule the cart has, mostly more than once.
 * The clamp to available stock was written three times ({@code addItem},
 * {@code updateQuantity}, {@code mergeCart}) and the "drop or clamp against current stock"
 * sweep a fourth. Two of those four also decided, separately, what an out-of-stock product
 * should say. There are no setters here and the rules are stated once each:
 *
 * <ul>
 *   <li><b>Ownership</b> is a {@link UserId} <em>xor</em> a guest id — never both, never
 *       neither. The old code could produce either, because {@code newCart} took both
 *       parameters and assigned whatever it was handed; nothing anywhere enforced it.</li>
 *   <li><b>A line's quantity</b> is positive and never exceeds available stock. Zero removes
 *       the line rather than leaving a dead one behind, which is the S8 {@code KNOWN BUG}
 *       about {@code updateQuantity(…, 0)}.</li>
 *   <li><b>Stock</b> is not this aggregate's to know. Every method that needs it takes a
 *       {@link CatalogSnapshot}, and the comparison itself belongs to the catalog — one rule,
 *       one place, which is the whole point of the sprint's first item.</li>
 * </ul>
 *
 * <h2>Persistence</h2>
 * Still a {@code @Document}, so the stored shape is untouched: {@code userId} is a bare string,
 * a line's {@code quantity} and {@code stock} bare ints. Spring Data instantiates through the
 * private no-arg constructor.
 */
@Getter
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    /**
     * Optimistic-locking token. The cart is written from several paths that each
     * read-modify-write the whole line list — add, change quantity, remove, the reconcile, and
     * the guest merge at login — so a concurrent pair (two tabs, or a merge racing an add)
     * could drop one side's changes entirely.
     *
     * <p>{@code @JsonIgnore}: internal, never on the wire, never client-settable.
     * Boxed, and backfilled to 0 by {@code data/seed/migrate.js} — see {@code Product}.
     */
    @Version
    @JsonIgnore
    private Long version;

    /** The owner, if this is a signed-in customer's cart. Exactly one of these two is set. */
    private UserId userId;

    /** The browser's opaque handle, if this is a guest's cart. */
    private String guestId;

    private List<CartLine> items = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Set by every method that changes something. Not stored and not serialized — it is a
     * fact about this instance's life in memory, not about the cart.
     */
    @org.springframework.data.annotation.Transient
    @JsonIgnore
    private transient boolean dirty;

    /** For Spring Data. */
    private Cart() {
    }

    // ------------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------------

    public static Cart forUser(UserId userId) {
        Cart cart = new Cart();
        cart.userId = Objects.requireNonNull(userId, "a user cart must have an owner");
        cart.open();
        return cart;
    }

    public static Cart forGuest(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            throw new IllegalArgumentException("a guest cart must have a guest id");
        }
        Cart cart = new Cart();
        cart.guestId = guestId;
        cart.open();
        return cart;
    }

    /**
     * The cart for whichever of the two identifiers is present.
     *
     * @throws IllegalArgumentException if both or neither are — the invariant the old
     *         {@code newCart(userId, guestId)} could silently break in either direction
     */
    public static Cart openFor(UserId userId, String guestId) {
        boolean hasUser = userId != null;
        boolean hasGuest = guestId != null && !guestId.isBlank();
        if (hasUser == hasGuest) {
            throw new IllegalArgumentException(
                    "a cart belongs to a user or to a guest, never both and never neither");
        }
        return hasUser ? forUser(userId) : forGuest(guestId);
    }

    // ------------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------------

    /** A read-only view — lines change through this aggregate's methods, never in place. */
    public List<CartLine> getItems() {
        return Collections.unmodifiableList(items);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Whether this aggregate holds a change that has not been written.
     *
     * <p>Only {@code getOrCreate} needs to ask: it is a read that occasionally has to persist
     * the result of reconciling, and there is no point issuing a write for a basket nothing
     * moved under. Every other path is a write and saves regardless.
     *
     * <p>{@code @JsonIgnore} because Jackson reads {@code isX()} as a property, and this
     * aggregate is compared field-for-field against its response DTO. That is the S10 lesson —
     * three properties appeared on every order response the moment the aggregate got behavior.
     */
    @JsonIgnore
    public boolean needsPersisting() {
        return dirty;
    }

    /** What the basket is worth before tax and shipping. */
    public Money subtotal() {
        return items.stream().map(CartLine::lineTotal).reduce(Money.ZERO, Money::plus);
    }

    /** The products this cart holds, for a caller that needs to fetch their snapshots. */
    public List<ProductId> productIds() {
        return items.stream().map(CartLine::getProductId).toList();
    }

    // ------------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------------

    /**
     * Put {@code quantity} more units of a product in the basket, adding to whatever is
     * already there.
     *
     * @throws com.dominator.gearly.catalog.domain.InsufficientStockException if the resulting
     *         total would exceed available stock — checked against the <em>total</em>, not
     *         against the increment, exactly as {@code addItem} did
     */
    public void addLine(CatalogSnapshot snapshot, Quantity quantity) {
        lineFor(snapshot.productId()).ifPresentOrElse(
                line -> line.increaseBy(quantity, snapshot),
                () -> items.add(CartLine.fromSnapshot(snapshot, quantity)));
        touch();
    }

    /**
     * Set a line to an absolute quantity.
     *
     * <p>Zero removes the line. The old {@code updateQuantity} left a quantity-0 line sitting
     * in the cart — an S8 {@code KNOWN BUG} — because nothing bounded the value below.
     *
     * <p>A product that is not in the cart is silently ignored, which is the behavior the
     * endpoint has always had.
     */
    public void changeQuantity(CatalogSnapshot snapshot, Quantity quantity) {
        lineFor(snapshot.productId()).ifPresent(line -> {
            if (quantity.isZero()) {
                items.remove(line);
            } else {
                line.changeQuantityTo(quantity, snapshot);
            }
        });
        touch();
    }

    public void removeLine(ProductId productId) {
        items.removeIf(line -> line.isFor(productId));
        touch();
    }

    /**
     * Take out the units that have just been bought, leaving anything the customer did not
     * order. A line reduced to nothing is dropped.
     *
     * <p>Driven by {@code OrderPlaced} — see {@code CartOrderListener}.
     */
    public void removeUnits(Map<ProductId, Quantity> bought) {
        if (bought == null || bought.isEmpty()) {
            return;
        }
        for (Iterator<CartLine> it = items.iterator(); it.hasNext(); ) {
            CartLine line = it.next();
            Quantity units = bought.get(line.getProductId());
            if (units == null) {
                continue;
            }
            line.decreaseBy(units);
            if (line.isEmpty()) {
                it.remove();
            }
        }
        touch();
    }

    public void clear() {
        items.clear();
        touch();
    }

    /**
     * Fold a guest's basket into this one at login.
     *
     * <p>Quantities for a product already here are added together and then clamped, as before.
     * What is new is that a product with no stock left is <em>skipped</em> rather than merged
     * in at quantity zero — {@code Math.min(incoming, 0)} with no guard, which the S8 suite
     * pinned as a {@code KNOWN BUG} because it left a dead line in the cart.
     */
    public void merge(Map<ProductId, Quantity> incoming, Map<ProductId, CatalogSnapshot> catalog) {
        incoming.forEach((productId, wanted) -> {
            CatalogSnapshot snapshot = catalog.get(productId);
            if (snapshot == null || snapshot.isOutOfStock()) {
                return;
            }
            lineFor(productId).ifPresentOrElse(
                    line -> line.changeQuantityTo(
                            snapshot.clamp(line.getQuantity().plus(wanted)), snapshot),
                    () -> items.add(CartLine.fromSnapshot(snapshot, snapshot.clamp(wanted))));
        });
        touch();
    }

    /**
     * Bring the whole basket back in line with the catalog: drop lines whose product is gone
     * or out of stock, clamp the rest, and refresh every line's stock hint.
     *
     * <p>This absorbs {@code syncCartWithStock}, and fixes the inconsistency the S8 suite
     * pinned: the old sweep only rewrote a line's {@code stock} when it also had to clamp the
     * quantity, so a cart that was still valid kept whatever stock figure it happened to be
     * created with — the storefront's quantity stepper was capped at a number that could be
     * months stale.
     *
     * @param catalog snapshots for the products this cart holds; a product absent from the map
     *                has been delisted and its line is dropped
     * @return whether anything changed, so the caller writes only when there is something to
     *         write. The old code saved here <em>and</em> again in the operation that followed
     *         — two round trips for one request.
     */
    public boolean reconcileWith(Map<ProductId, CatalogSnapshot> catalog) {
        boolean modified = false;

        for (Iterator<CartLine> it = items.iterator(); it.hasNext(); ) {
            CartLine line = it.next();
            CatalogSnapshot snapshot = catalog.get(line.getProductId());

            if (snapshot == null || snapshot.isOutOfStock()) {
                it.remove();
                modified = true;
                continue;
            }
            modified |= line.reconcileWith(snapshot);
        }

        if (modified) {
            touch();
        }
        return modified;
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    private Optional<CartLine> lineFor(ProductId productId) {
        return items.stream().filter(line -> line.isFor(productId)).findFirst();
    }

    /**
     * Stamps {@code updatedAt}. {@code @LastModifiedDate} would do this on save anyway, but
     * {@code CartService.saveCart} did it by hand and the characterization suite observes it
     * before the save, so the aggregate keeps doing it at the moment the change happens.
     */
    private void touch() {
        this.updatedAt = Instant.now();
        this.dirty = true;
    }

    private void open() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

}
