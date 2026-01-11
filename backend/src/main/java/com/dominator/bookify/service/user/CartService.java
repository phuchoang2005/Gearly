package com.dominator.bookify.service.user;

import com.dominator.bookify.model.Book;
import com.dominator.bookify.model.Cart;
import com.dominator.bookify.model.CartItem;
import com.dominator.bookify.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final BookService bookService;

    public Cart getOrCreate(String userId, String guestId) {
        Cart cart = (userId != null
                ? cartRepository.findByUserId(userId).orElseGet(() -> newCart(userId, null))
                : cartRepository.findByGuestId(guestId).orElseGet(() -> newCart(null, guestId))
        );
        syncCartWithStock(cart);
        return cart;
    }

    private void syncCartWithStock(Cart cart) {
        boolean modified = false;

        for (Iterator<CartItem> it = cart.getItems().iterator(); it.hasNext();) {
            CartItem item = it.next();
            Optional<Book> optBook = Optional.ofNullable(bookService.getBookById(item.getBookId()));
            if (optBook.isEmpty()) {
                it.remove();
                modified = true;
                continue;
            }
            Book book = optBook.get();
            int currentStock = book.getStock();
            if (currentStock  <= 0) {
                it.remove();
                modified = true;
            } else if (item.getQuantity() > currentStock) {
                item.setQuantity(currentStock);
                item.setStock(currentStock);
                modified = true;
            }
        }

        if (modified) {
            cartRepository.save(cart);
        }
    }


    private Cart newCart(String userId, String guestId) {
        Cart c = new Cart();
        c.setUserId(userId);
        c.setGuestId(guestId);
        c.setItems(new ArrayList<>());
        c.setCreatedAt(new Date());
        c.setUpdatedAt(new Date());
        return cartRepository.save(c);
    }

    private Cart saveCart(Cart c) {
        c.setUpdatedAt(new Date());
        return cartRepository.save(c);
    }

    public Cart addItem(String userId, String guestId, CartItem item) {
        if (item == null) throw new IllegalArgumentException("CartItem must not be null");

        Cart cart = getOrCreate(userId, guestId);
        CartItem existing = findItemInCart(cart, item.getBookId());

        int stock = bookService.getStock(item.getBookId());
        int addedQty = item.getQuantity();
        int newQty = (existing != null ? existing.getQuantity() : 0) + addedQty;

        if (newQty > stock) {
            String errorMsg = "Only " + stock + " Left!";
            if (stock == 0) {
                errorMsg = "This item is out of stock!";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    errorMsg);
        }

        if (existing != null) {
            existing.setQuantity(newQty);
        } else {
            cart.getItems().add(item);
        }

        return saveCart(cart);
    }

    public Cart updateQuantity(String userId, String guestId, String bookId, int qty) {
        int stock = bookService.getStock(bookId);
        if (qty > stock) {
            String errorMsg = "Only " + stock + " Left!";
            if (stock == 0) {
                errorMsg = "This item is out of stock!";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    errorMsg);
        }

        Cart cart = getOrCreate(userId, guestId);
        CartItem existing = findItemInCart(cart, bookId);
        if (existing != null) {
            existing.setQuantity(qty);
        }

        return saveCart(cart);
    }

    public void removeItem(String userId, String guestId, String bookId) {
        Cart cart = getOrCreate(userId, guestId);
        cart.setItems(cart.getItems().stream()
                .filter(i -> !i.getBookId().equals(bookId))
                .collect(Collectors.toList()));
        saveCart(cart);
    }

    public void removeItems(String userId, String guestId, Map<String, Integer> removeQuantities) {
        if (removeQuantities == null || removeQuantities.isEmpty()) return;

        Cart cart = getOrCreate(userId, guestId);
        List<CartItem> updatedItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            String bookId = item.getBookId();
            Integer qtyToRemove = removeQuantities.get(bookId);

            if (qtyToRemove != null && qtyToRemove > 0) {
                int existingQty = item.getQuantity();
                if (qtyToRemove >= existingQty) {
                    continue;
                } else {
                    item.setQuantity(existingQty - qtyToRemove);
                }
            }

            updatedItems.add(item);
        }

        cart.setItems(updatedItems);
        saveCart(cart);
    }


    public void clearCart(String userId, String guestId) {
        Cart cart = getOrCreate(userId, guestId);
        cart.getItems().clear();
        saveCart(cart);
    }

    public Cart mergeCart(String userId, String guestId, List<CartItem> localItems) {
        Cart userCart = getOrCreate(userId, null);

        for (CartItem incoming : localItems) {
            if (incoming == null) continue;

            int stock = bookService.getStock(incoming.getBookId());
            int qty = Math.min(incoming.getQuantity(), stock);

            CartItem existing = findItemInCart(userCart, incoming.getBookId());
            if (existing != null) {
                int totalQty = Math.min(existing.getQuantity() + qty, stock);
                existing.setQuantity(totalQty);
            } else {
                incoming.setQuantity(qty);
                userCart.getItems().add(incoming);
            }
        }

        saveCart(userCart);
        cartRepository.deleteByGuestId(guestId);
        return userCart;
    }

    public Cart addItems(String userId, String guestId, List<String> itemIds) {
        Cart cart = getOrCreate(userId, guestId);

        for (String bookId : itemIds) {
            if (bookId == null) continue;

            var book = bookService.getBookById(bookId);
            if (book == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found: " + bookId);
            }

            CartItem item = getCartItem(book);

            CartItem existing = findItemInCart(cart, item.getBookId());
            int newQty = (existing != null ? existing.getQuantity() : 0) + 1;

            int stock = book.getStock();
            if (newQty > stock) {
                String errorMsg = "Only " + stock + " Left for ";
                if (stock == 0) {
                    errorMsg = "\"" + book.getTitle() +  "\" is out of stock!";
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        errorMsg);
            }

            if (existing != null) {
                existing.setQuantity(newQty);
            } else {
                cart.getItems().add(item);
            }
        }

        return saveCart(cart);
    }

    private static CartItem getCartItem(Book book) {
        CartItem item = new CartItem();
        item.setBookId(book.getId());
        item.setTitle(book.getTitle());
        item.setAuthor(book.getAuthors() != null && !book.getAuthors().isEmpty() ? book.getAuthors().getFirst() : "Unknown");
        item.setPrice(book.getPrice());
        item.setQuantity(1);
        item.setImage((book.getImages() != null && !book.getImages().isEmpty()) ? book.getImages().get(0).getUrl() : null);
        item.setCondition(book.getCondition());
        item.setStock(book.getStock());
        return item;
    }


    // Utility
    private CartItem findItemInCart(Cart cart, String bookId) {
        return cart.getItems().stream()
                .filter(i -> i.getBookId().equals(bookId))
                .findFirst()
                .orElse(null);
    }

}