package com.dominator.gearly.identity.application;

import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The customer's saved products — the writes.
 *
 * <h2>Why the wishlist is still not its own aggregate</h2>
 * S11 made that call deliberately and wrote out the reasoning; S12 has not changed it. It is a
 * set of ids with no invariant of its own, it is never changed without its owner being loaded
 * and saved, and it holds no catalog object — so an aggregate boundary around it would protect
 * nothing and would turn each of these four operations into two writes across two aggregates.
 * The one argument for splitting it is size: an unbounded array inside the document every
 * request loads. That is a real limit, but it is a scaling decision with a migration behind it
 * rather than a modelling mistake, and it stays <b>logged as a follow-up</b>.
 *
 * <h2>Why the page that displays it is not here</h2>
 * Because it is a catalog query. Rendering a wishlist means filtering, sorting and paging
 * product cards, which is {@code ProductQueryService}'s job and produces catalog DTOs — so
 * {@code GET /api/wishlist} lives in {@code catalog.api.WishlistProductsController} and reaches
 * back for the ids through {@code UserDirectory.favoritesOf}, identity's published port. This
 * class owns the four operations that actually change a {@code User}.
 *
 * <p>The split is what {@code contexts_touch_each_other_only_through_published_types} requires,
 * and it is the arrangement the old code's own comment described — "the favourites list is the
 * user's; turning ids into catalog cards is the catalog's" — while injecting a catalog
 * application service to do both.
 *
 * <p>The set operations themselves moved onto the aggregate. Four call sites each rebuilt a
 * {@code HashSet} from the stored list to get "no duplicates", and each handed the resulting
 * mutable list straight back to the entity.
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final UserRepository users;

    public void addToWishlist(UserId caller, ProductId productId) {
        User user = require(caller);
        user.addFavorite(productId);
        users.save(user);
    }

    public void removeFromWishlist(UserId caller, ProductId productId) {
        User user = require(caller);
        user.removeFavorite(productId);
        users.save(user);
    }

    /**
     * Fold a guest's saved products into the account's on sign-in, and return the result.
     *
     * <p>The returned set is what the storefront replaces its local list with, so it is built
     * from what was saved rather than from what was sent.
     */
    public Set<String> mergeWishlist(UserId caller, List<ProductId> productIds) {
        User user = require(caller);
        user.addFavorites(productIds);
        users.save(user);
        return asStrings(user);
    }

    public void bulkRemoveFromWishlist(UserId caller, List<ProductId> productIds) {
        User user = require(caller);
        user.removeFavorites(productIds);
        users.save(user);
    }

    private User require(UserId caller) {
        return users.findById(caller).orElseThrow(() -> new UserNotFoundException(caller));
    }

    /** The wire form: bare id strings, in the order the aggregate holds them. */
    private Set<String> asStrings(User user) {
        Set<String> ids = new LinkedHashSet<>();
        user.getFavorites().forEach(id -> ids.add(id.value()));
        return ids;
    }
}
