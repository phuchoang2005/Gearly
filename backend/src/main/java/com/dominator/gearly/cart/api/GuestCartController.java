package com.dominator.gearly.cart.api;

import com.dominator.gearly.cart.application.CartService;
import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.cart.domain.GuestCartIds;
import com.dominator.gearly.cart.domain.UnknownGuestCartException;
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

/**
 * A not-signed-in visitor's cart, keyed by a UUID the browser holds. Same six routes.
 *
 * <h2>S12: the id is bound to this server</h2>
 * This used to be {@code permitAll} with a client-supplied {@code guestId} and no binding of
 * any kind — anyone who learned a UUID could read and mutate that basket, and any string at all
 * created one. Every route below now runs the id through {@link GuestCartIds#verify} first, so
 * the only ids that reach {@code CartService} are ones {@link #init()} signed. See
 * {@link GuestCartIds} for what that does and does not claim.
 *
 * <p>{@code SecurityConfig} is unchanged and these routes stay {@code permitAll}: a guest has no
 * account, so there is nothing for the filter chain to authenticate. The check belongs here,
 * where the id arrives.
 *
 * <p><b>One visible consequence.</b> A returning visitor holding a pre-S12 bare UUID is refused
 * with a 403 once; the storefront drops it and re-inits. See {@link UnknownGuestCartException}.
 */
@RestController
@RequestMapping("/api/guest-cart")
@RequiredArgsConstructor
public class GuestCartController {

    private final CartService cartService;
    private final CartResponseMapper cartMapper;

    private final GuestCartIds guestCartIds;

    /**
     * Hands out a signed id and the empty basket behind it. The response shape is unchanged —
     * {@code {"guestId": "…"}} — and the storefront already treats the value as opaque, which
     * is why it needs no change to accept the new format.
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> init() {
        String issued = guestCartIds.issue();
        cartService.getOrCreate(null, unwrap(issued));
        return ResponseEntity.ok(Map.of("guestId", issued));
    }

    @GetMapping
    public ResponseEntity<CartResponseDTO> get(@RequestParam String guestId) {
        return ok(cartService.getOrCreate(null, unwrap(guestId)));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> add(@RequestParam String guestId,
                                               @RequestBody @Valid AddCartItemRequestDTO request) {
        return ok(cartService.addItem(null, unwrap(guestId),
                ProductId.of(request.getProductId()), Quantity.of(request.getQuantity())));
    }

    @PutMapping("/update")
    public ResponseEntity<CartResponseDTO> update(@RequestParam String guestId,
                                                  @RequestParam String productId,
                                                  @RequestParam int quantity) {
        return ok(cartService.updateQuantity(null, unwrap(guestId), ProductId.of(productId), quantity));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> remove(@RequestParam String guestId,
                                       @RequestParam String productId) {
        cartService.removeItem(null, unwrap(guestId), ProductId.of(productId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@RequestParam String guestId) {
        cartService.clearCart(null, unwrap(guestId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<CartResponseDTO> bulkAdd(@RequestParam String guestId,
                                                   @RequestBody List<String> itemIds) {
        return ok(cartService.addItems(null, unwrap(guestId), itemIds));
    }

    /**
     * The signed id a client presented, unwrapped — or a 403.
     *
     * <p>Every route goes through here, which is the point: an id that reaches
     * {@code CartService} has been verified, and there is no way to write a route that forgets
     * to, short of not calling this.
     */
    private String unwrap(String presented) {
        return guestCartIds.verify(presented).orElseThrow(UnknownGuestCartException::new);
    }

    private ResponseEntity<CartResponseDTO> ok(Cart cart) {
        return ResponseEntity.ok(cartMapper.toResponseDto(cart));
    }
}
