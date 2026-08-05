package com.dominator.gearly.cart.api;

import com.dominator.gearly.cart.application.CartService;
import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A not-signed-in visitor's cart, keyed by a UUID the browser holds. Same six routes.
 *
 * <p>Note what this class still is: {@code /api/guest-cart/**} is {@code permitAll} with a
 * client-supplied {@code guestId} and no binding of any kind, so anyone who learns a UUID can
 * read and mutate that basket. That is one of the four security items S12 owns, and it is
 * untouched here — moving a hole is not fixing it. What S11 does fix is the one that lived in
 * the request body: see {@link AddCartItemRequestDTO}.
 */
@RestController
@RequestMapping("/api/guest-cart")
@RequiredArgsConstructor
public class GuestCartController {

    private final CartService cartService;
    private final CartResponseMapper cartMapper;

    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> init() {
        String guestId = UUID.randomUUID().toString();
        cartService.getOrCreate(null, guestId);
        return ResponseEntity.ok(Map.of("guestId", guestId));
    }

    @GetMapping
    public ResponseEntity<CartResponseDTO> get(@RequestParam String guestId) {
        return ok(cartService.getOrCreate(null, guestId));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> add(@RequestParam String guestId,
                                               @RequestBody @Valid AddCartItemRequestDTO request) {
        return ok(cartService.addItem(null, guestId,
                ProductId.of(request.getProductId()), Quantity.of(request.getQuantity())));
    }

    @PutMapping("/update")
    public ResponseEntity<CartResponseDTO> update(@RequestParam String guestId,
                                                  @RequestParam String productId,
                                                  @RequestParam int quantity) {
        return ok(cartService.updateQuantity(null, guestId, ProductId.of(productId), quantity));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> remove(@RequestParam String guestId,
                                       @RequestParam String productId) {
        cartService.removeItem(null, guestId, ProductId.of(productId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@RequestParam String guestId) {
        cartService.clearCart(null, guestId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<CartResponseDTO> bulkAdd(@RequestParam String guestId,
                                                   @RequestBody List<String> itemIds) {
        return ok(cartService.addItems(null, guestId, itemIds));
    }

    private ResponseEntity<CartResponseDTO> ok(Cart cart) {
        return ResponseEntity.ok(cartMapper.toResponseDto(cart));
    }
}
