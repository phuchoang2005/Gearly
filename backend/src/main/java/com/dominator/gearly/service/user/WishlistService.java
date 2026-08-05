package com.dominator.gearly.service.user;

import com.dominator.gearly.catalog.api.ProductSummaryDTO;
import com.dominator.gearly.catalog.application.ProductQueryService;
import com.dominator.gearly.dto.WishlistRequestDTO;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * The customer's saved products.
 *
 * <h2>Why the wishlist is not its own aggregate — a deliberate choice, S11</h2>
 * It lives as a {@code List<String>} of product ids on {@code User.favorites}, and it stays
 * there. The plan asked for that decision to be made explicitly rather than left as an
 * accident of the original schema, so here it is:
 *
 * <ul>
 *   <li><b>It has no invariant of its own.</b> A wishlist is a set of ids with no rule about
 *       size, order, or what may be in it — nothing an aggregate boundary would protect. The
 *       one rule it does have, "no duplicates", is a property of the set.</li>
 *   <li><b>It is never changed without its owner.</b> Every operation here starts from the
 *       authenticated user and ends by saving that user, so "one aggregate per transaction"
 *       is already satisfied. Splitting it would turn each of these into two writes across two
 *       aggregates and buy a consistency problem that does not currently exist.</li>
 *   <li><b>It does not reference the catalog by object.</b> The ids are typed at the boundary
 *       and resolved through {@code ProductQueryService}, so the coupling an aggregate split
 *       usually exists to break is already absent.</li>
 * </ul>
 *
 * <p>The case for splitting it is size: an unbounded array inside the user document grows the
 * document every customer's every request loads. That is a real limit and a real reason to
 * revisit this, but it is a scaling decision with a migration behind it, not a modelling
 * mistake to correct in a refactor. <b>Logged as a follow-up</b>, alongside
 * {@code ChatMemoryService}'s JVM-local map, for whoever owns capacity.
 */
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final UserRepository userRepository;
    private final ProductQueryService productQueryService;

    /**
     * The favourites list is the user's; turning ids into catalog cards is the catalog's. The
     * filtering, sorting and paging that used to be written out here moved into
     * {@code ProductQueryService.getProductsByIds}, which is now the single implementation
     * behind both this page and {@code GET /api/products}.
     */
    public Page<ProductSummaryDTO> getWishlist(AuthenticatedUser authUser, WishlistRequestDTO dto) {
        User user = authUser.getUser();
        List<String> favorites = Optional.ofNullable(user.getFavorites()).orElse(Collections.emptyList());

        if (favorites.isEmpty()) return Page.empty();

        return productQueryService.getProductsByIds(
                favorites, dto.getSearchTxt(), dto.getPageIndex(), dto.getPageSize(), true);
    }

    public void addToWishlist(AuthenticatedUser authUser, String productId) {
        User user = authUser.getUser();
        Set<String> favorites = new HashSet<>(Optional.ofNullable(user.getFavorites()).orElse(List.of()));
        favorites.add(productId);
        user.setFavorites(new ArrayList<>(favorites));
        userRepository.save(user);
    }

    public void removeFromWishlist(AuthenticatedUser authUser, String productId) {
        User user = authUser.getUser();
        List<String> updated = Optional.ofNullable(user.getFavorites()).orElse(List.of())
                .stream().filter(id -> !id.equals(productId)).toList();
        user.setFavorites(updated);
        userRepository.save(user);
    }

    public Set<String> mergeWishlist(AuthenticatedUser authUser, List<String> productIds) {
        User user = authUser.getUser();
        Set<String> merged = new HashSet<>(Optional.ofNullable(user.getFavorites()).orElse(List.of()));
        merged.addAll(productIds);
        user.setFavorites(new ArrayList<>(merged));
        userRepository.save(user);
        return merged;
    }

    public void bulkRemoveFromWishlist(AuthenticatedUser authUser, List<String> productIds) {
        User user = authUser.getUser();
        List<String> updated = new ArrayList<>(Optional.ofNullable(user.getFavorites()).orElse(List.of()));

        updated.removeAll(productIds);
        user.setFavorites(updated);
        userRepository.save(user);
    }
}
