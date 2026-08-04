package com.dominator.gearly.service.user;

import com.dominator.gearly.mapper.CartMapper;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Cart;
import com.dominator.gearly.model.CartItem;
import com.dominator.gearly.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductService productService;
    private final CartMapper cartMapper;

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
            Optional<Product> optProduct = Optional.ofNullable(productService.getProductById(item.getProductId()));
            if (optProduct.isEmpty()) {
                it.remove();
                modified = true;
                continue;
            }
            Product product = optProduct.get();
            int currentStock = product.getStock();
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
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return cartRepository.save(c);
    }

    private Cart saveCart(Cart c) {
        c.setUpdatedAt(Instant.now());
        return cartRepository.save(c);
    }

    public Cart addItem(String userId, String guestId, CartItem item) {
        if (item == null) throw new IllegalArgumentException("CartItem must not be null");

        Cart cart = getOrCreate(userId, guestId);
        CartItem existing = findItemInCart(cart, item.getProductId());

        int stock = productService.getStock(item.getProductId());
        int addedQty = item.getQuantity();
        int newQty = (existing != null ? existing.getQuantity() : 0) + addedQty;

        if (newQty > stock) {
            String errorMsg = "Only " + stock + " Left!";
            if (stock == 0) {
                errorMsg = "This item is out of stock!";
            }
            throw new BadRequestException(
                    errorMsg);
        }

        if (existing != null) {
            existing.setQuantity(newQty);
        } else {
            cart.getItems().add(item);
        }

        return saveCart(cart);
    }

    public Cart updateQuantity(String userId, String guestId, String productId, int qty) {
        int stock = productService.getStock(productId);
        if (qty > stock) {
            String errorMsg = "Only " + stock + " Left!";
            if (stock == 0) {
                errorMsg = "This item is out of stock!";
            }
            throw new BadRequestException(
                    errorMsg);
        }

        Cart cart = getOrCreate(userId, guestId);
        CartItem existing = findItemInCart(cart, productId);
        if (existing != null) {
            existing.setQuantity(qty);
        }

        return saveCart(cart);
    }

    public void removeItem(String userId, String guestId, String productId) {
        Cart cart = getOrCreate(userId, guestId);
        cart.setItems(cart.getItems().stream()
                .filter(i -> !i.getProductId().equals(productId))
                .collect(Collectors.toList()));
        saveCart(cart);
    }

    public void removeItems(String userId, String guestId, Map<String, Integer> removeQuantities) {
        if (removeQuantities == null || removeQuantities.isEmpty()) return;

        Cart cart = getOrCreate(userId, guestId);
        List<CartItem> updatedItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            String productId = item.getProductId();
            Integer qtyToRemove = removeQuantities.get(productId);

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

    public void deleteGuestCart(String guestId) {
        cartRepository.deleteByGuestId(guestId);
    }

    public Cart mergeCart(String userId, String guestId, List<CartItem> localItems) {
        Cart userCart = getOrCreate(userId, null);

        for (CartItem incoming : localItems) {
            if (incoming == null) continue;

            int stock = productService.getStock(incoming.getProductId());
            int qty = Math.min(incoming.getQuantity(), stock);

            CartItem existing = findItemInCart(userCart, incoming.getProductId());
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

        for (String productId : itemIds) {
            if (productId == null) continue;

            var product = productService.getProductById(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }

            CartItem item = cartMapper.toCartItem(product);

            CartItem existing = findItemInCart(cart, item.getProductId());
            int newQty = (existing != null ? existing.getQuantity() : 0) + 1;

            int stock = product.getStock();
            if (newQty > stock) {
                String errorMsg = "Only " + stock + " Left for ";
                if (stock == 0) {
                    errorMsg = "\"" + product.getTitle() +  "\" is out of stock!";
                }
                throw new BadRequestException(
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

    // Utility
    private CartItem findItemInCart(Cart cart, String productId) {
        return cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

}