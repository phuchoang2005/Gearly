package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.dto.WishlistRequestDTO;
import com.dominator.gearly.mapper.ProductMapper;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public Page<ProductSummaryDTO> getWishlist(AuthenticatedUser authUser, WishlistRequestDTO dto) {
        User user = authUser.getUser();
        List<String> favorites = Optional.ofNullable(user.getFavorites()).orElse(Collections.emptyList());

        if (favorites.isEmpty()) return Page.empty();

        List<Product> products = productRepository.findAllById(favorites);
        String keyword = dto.getSearchTxt().trim().toLowerCase();

        List<ProductSummaryDTO> filtered = products.stream()
                .filter(product -> keyword.isEmpty() ||
                        product.getTitle().toLowerCase().contains(keyword) ||
                        product.getAuthors().stream().anyMatch(a -> a.toLowerCase().contains(keyword)))
                .sorted(Comparator.comparing(Product::getTitle))
                .map(productMapper::toSummaryDto)
                .toList();

        int start = dto.getPageIndex() * dto.getPageSize();
        int end = Math.min(start + dto.getPageSize(), filtered.size());
        if (start >= end) return Page.empty();

        Pageable pageable = PageRequest.of(dto.getPageIndex(), dto.getPageSize(), Sort.by(Sort.Direction.ASC, "title"));
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
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
