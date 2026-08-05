package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Identity's anti-corruption layer.</b> What another context is allowed to know about a
 * person: the name to print beside their review or their order, and nothing else.
 *
 * <p>The counterpart of the catalog's {@code ProductSnapshotPort}, and there for the same
 * reason. The reviews context needs a display name on every row of the moderation screen, and
 * before this it got one by injecting the Spring Data {@code UserRepository} and calling
 * {@code User::getFullName} — which handed a whole aggregate, password hash included, to a
 * context with no business holding one. Two fitness functions say so:
 * {@code contexts_touch_each_other_only_through_published_types} (a {@code User} is not
 * identity's published language) and {@code spring_data_repositories_live_only_in_infrastructure}.
 *
 * <p>Names are {@link String}s rather than {@link com.dominator.gearly.shared.domain.PersonName}s
 * on purpose: an externally-authenticated account can carry a one-word display name with no
 * first/last parts, so a {@code PersonName} would be absent for exactly the accounts whose name
 * is most likely to be shown. The empty {@link Optional} means "no such account", which is what
 * each caller phrases in its own way — {@code "Unknown User"} on the storefront, {@code "—"} on
 * the admin table.
 */
public interface UserDirectory {

    /** The display name of one account. */
    Optional<String> displayNameOf(UserId userId);

    /**
     * Display names for a batch, so a table of reviews costs one query rather than one per row.
     * Ids with no account are absent from the result rather than mapping to null.
     */
    Map<UserId, String> displayNamesOf(Collection<UserId> userIds);

    /**
     * The products this person has saved.
     *
     * <p>Published because the wishlist <em>page</em> is a catalog query — a page of product
     * cards, filtered and sorted and paged like any other — while the wishlist <em>itself</em>
     * is a field on the {@code User} aggregate. Somebody has to cross that line, and this is the
     * direction that costs less: the catalog reads a list of {@link ProductId}s, which is
     * shared-kernel vocabulary, rather than identity learning how to render a product card.
     *
     * <p>Read-only on purpose. Adding and removing favourites stays on the aggregate and is
     * reached through {@code identity.api.WishlistController}; a write method here would make
     * the catalog the place that changes identity's state, which is the distributed-monolith
     * shape S11 broke {@code OrderPlacedListener} apart to avoid.
     *
     * @return empty for an unknown account, or for one that has saved nothing
     */
    List<ProductId> favoritesOf(UserId userId);
}
