package com.dominator.gearly.cart.application;

import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.cart.domain.CartRepository;
import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The cart use cases. Loads the basket, asks the catalog what it needs to know, tells the
 * aggregate what happened, and writes once.
 *
 * <p>What is <em>not</em> here is the point. The 237-line service this replaces held the stock
 * clamp three times over, decided twice what an out-of-stock product should say, and reached
 * into a {@code Product} for seven fields. All of that is {@code Cart} and
 * {@code CatalogSnapshot} now, which is why every method below is a handful of lines: fetch,
 * delegate, save.
 *
 * <h2>One write per request</h2>
 * The old code saved the cart in {@code syncCartWithStock} and then again in whatever
 * operation followed — two round trips for one call, which the S8 suite pinned as a fact worth
 * knowing before this rewrite. {@link Cart#reconcileWith} reports whether it changed anything
 * and the write happens once, at the end.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository carts;
    private final ProductSnapshotPort catalog;

    // ------------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------------

    /**
     * The caller's basket, reconciled against the catalog: lines whose product has been
     * delisted or sold out are dropped, the rest are clamped to what is available, and every
     * line's stock hint is refreshed.
     *
     * <p>This is the one path that writes on a read, and only when the reconcile actually
     * changed something — a basket the world has not moved under costs one query and nothing
     * else. The write paths below use {@link #loadReconciled} instead and save once at the
     * end, which is what turns the old two-round-trip request into one.
     */
    public Cart getOrCreate(UserId userId, String guestId) {
        Cart cart = loadReconciled(userId, guestId);
        return cart.needsPersisting() ? carts.save(cart) : cart;
    }

    // ------------------------------------------------------------------------
    // Writes
    // ------------------------------------------------------------------------

    /** Add units of one product, on top of whatever is already in the basket. */
    public Cart addItem(UserId userId, String guestId, ProductId productId, Quantity quantity) {
        Cart cart = loadReconciled(userId, guestId);
        cart.addLine(catalog.snapshotOf(productId), quantity);
        return carts.save(cart);
    }

    /** Add one unit each of several products — the storefront's "add all to cart". */
    public Cart addItems(UserId userId, String guestId, List<String> productIds) {
        Cart cart = loadReconciled(userId, guestId);

        for (String rawId : productIds) {
            if (rawId == null) {
                continue;
            }
            cart.addLine(catalog.snapshotOf(ProductId.of(rawId)), Quantity.ONE);
        }
        return carts.save(cart);
    }

    /** Set a line to an absolute quantity. Zero removes it. */
    public Cart updateQuantity(UserId userId, String guestId, ProductId productId, int quantity) {
        Cart cart = loadReconciled(userId, guestId);
        cart.changeQuantity(catalog.snapshotOf(productId), Quantity.of(quantity));
        return carts.save(cart);
    }

    public void removeItem(UserId userId, String guestId, ProductId productId) {
        Cart cart = loadReconciled(userId, guestId);
        cart.removeLine(productId);
        carts.save(cart);
    }

    /**
     * Take out the units an order has just consumed. Driven by {@code OrderPlaced} — see
     * {@code CartOrderListener}.
     */
    public void removeItems(UserId userId, String guestId, Map<ProductId, Quantity> bought) {
        if (bought == null || bought.isEmpty()) {
            return;
        }
        Cart cart = loadReconciled(userId, guestId);
        cart.removeUnits(bought);
        carts.save(cart);
    }

    public void clearCart(UserId userId, String guestId) {
        Cart cart = loadReconciled(userId, guestId);
        cart.clear();
        carts.save(cart);
    }

    public void deleteGuestCart(String guestId) {
        carts.deleteByGuest(guestId);
    }

    /**
     * Fold a guest's basket into the user's at login, then discard the guest cart.
     *
     * <p>The incoming quantities are all the guest cart contributes. Everything else about the
     * resulting lines — title, price, image, condition — is read from the catalog, so a client
     * cannot smuggle a price in through the merge endpoint any more than through the add one.
     */
    public Cart mergeCart(UserId userId, String guestId, Map<ProductId, Quantity> incoming) {
        Cart userCart = loadReconciled(userId, null);

        userCart.merge(incoming, snapshotsOf(incoming.keySet().stream().toList()));

        Cart merged = carts.save(userCart);
        carts.deleteByGuest(guestId);
        return merged;
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    /**
     * The basket, brought in line with the catalog but not yet written.
     *
     * <p>One catalog read for the whole cart, where the old sweep made one per line. An empty
     * basket skips the read entirely — there is nothing to reconcile it against.
     */
    private Cart loadReconciled(UserId userId, String guestId) {
        Cart cart = userId != null
                ? carts.findByUser(userId).orElseGet(() -> carts.save(Cart.forUser(userId)))
                : carts.findByGuest(guestId).orElseGet(() -> carts.save(Cart.forGuest(guestId)));

        if (!cart.isEmpty()) {
            cart.reconcileWith(snapshotsOf(cart.productIds()));
        }
        return cart;
    }

    private Map<ProductId, CatalogSnapshot> snapshotsOf(List<ProductId> productIds) {
        Map<ProductId, CatalogSnapshot> byId = new LinkedHashMap<>();
        catalog.snapshotsOf(productIds).forEach(snapshot -> byId.put(snapshot.productId(), snapshot));
        return byId;
    }
}
