package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.BookSummaryDTO;
import com.dominator.bookify.dto.WishlistRequestDTO;
import com.dominator.bookify.model.Book;
import com.dominator.bookify.model.User;
import com.dominator.bookify.repository.BookRepository;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public Page<BookSummaryDTO> getWishlist(AuthenticatedUser authUser, WishlistRequestDTO dto) {
        User user = authUser.getUser();
        List<String> favorites = Optional.ofNullable(user.getFavorites()).orElse(Collections.emptyList());

        if (favorites.isEmpty()) return Page.empty();

        List<Book> books = bookRepository.findAllById(favorites);
        String keyword = dto.getSearchTxt().trim().toLowerCase();

        List<BookSummaryDTO> filtered = books.stream()
                .filter(book -> keyword.isEmpty() ||
                        book.getTitle().toLowerCase().contains(keyword) ||
                        book.getAuthors().stream().anyMatch(a -> a.toLowerCase().contains(keyword)))
                .sorted(Comparator.comparing(Book::getTitle))
                .map(book -> new BookSummaryDTO(
                        book.getId(),
                        book.getTitle(),
                        book.getAuthors(),
                        book.getPrice(),
                        book.getStock(),
                        book.getCondition(),
                        book.getAverageRating(),
                        book.getRatingCount(),
                        book.getTotalRating(),
                        book.getImages()
                ))
                .toList();

        int start = dto.getPageIndex() * dto.getPageSize();
        int end = Math.min(start + dto.getPageSize(), filtered.size());
        if (start >= end) return Page.empty();

        Pageable pageable = PageRequest.of(dto.getPageIndex(), dto.getPageSize(), Sort.by(Sort.Direction.ASC, "title"));
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    public void addToWishlist(AuthenticatedUser authUser, String bookId) {
        User user = authUser.getUser();
        Set<String> favorites = new HashSet<>(Optional.ofNullable(user.getFavorites()).orElse(List.of()));
        favorites.add(bookId);
        user.setFavorites(new ArrayList<>(favorites));
        userRepository.save(user);
    }

    public void removeFromWishlist(AuthenticatedUser authUser, String bookId) {
        User user = authUser.getUser();
        List<String> updated = Optional.ofNullable(user.getFavorites()).orElse(List.of())
                .stream().filter(id -> !id.equals(bookId)).toList();
        user.setFavorites(updated);
        userRepository.save(user);
    }

    public Set<String> mergeWishlist(AuthenticatedUser authUser, List<String> bookIds) {
        User user = authUser.getUser();
        Set<String> merged = new HashSet<>(Optional.ofNullable(user.getFavorites()).orElse(List.of()));
        merged.addAll(bookIds);
        user.setFavorites(new ArrayList<>(merged));
        userRepository.save(user);
        return merged;
    }

    public void bulkRemoveFromWishlist(AuthenticatedUser authUser, List<String> bookIds) {
        User user = authUser.getUser();
        List<String> updated = new ArrayList<>(Optional.ofNullable(user.getFavorites()).orElse(List.of()));

        updated.removeAll(bookIds);
        user.setFavorites(updated);
        userRepository.save(user);
    }
}
