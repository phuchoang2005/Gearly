package com.dominator.gearly.cart.api;

import com.dominator.gearly.cart.application.CartService;
import com.dominator.gearly.cart.domain.GuestCartIds;
import com.dominator.gearly.cart.domain.UnknownGuestCartException;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A signed-in customer's cart. Same seven routes, same response shape.
 *
 * <p>The controller unwraps the principal into a {@link UserId} and passes that, so a Spring
 * Security type never reaches a use case — the rule ArchUnit's
 * {@code security_types_stop_at_the_api_layer} enforces, and which this controller already
 * came closest to obeying before the move.
 *
 * <p>Two of the seven take a guest id — the sign-in merge and the cleanup that follows it — and
 * both verify it the same way {@code GuestCartController} does. They are the paths that would
 * otherwise have left the S12 binding half-applied: {@code /api/cart/merge} reads a guest basket
 * and would have read any basket named by any string.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartResponseMapper cartMapper;
    private final GuestCartIds guestCartIds;

    @GetMapping
    public ResponseEntity<CartResponseDTO> get(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ok(cartService.getOrCreate(buyer(authUser), null));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> add(@AuthenticationPrincipal AuthenticatedUser authUser,
                                               @RequestBody @Valid AddCartItemRequestDTO request) {
        return ok(cartService.addItem(buyer(authUser), null,
                ProductId.of(request.getProductId()), Quantity.of(request.getQuantity())));
    }

    @PutMapping("/update")
    public ResponseEntity<CartResponseDTO> update(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                  @RequestParam String productId,
                                                  @RequestParam int quantity) {
        return ok(cartService.updateQuantity(buyer(authUser), null, ProductId.of(productId), quantity));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser authUser,
                                       @RequestParam String productId) {
        cartService.removeItem(buyer(authUser), null, ProductId.of(productId));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthenticatedUser authUser) {
        cartService.clearCart(buyer(authUser), null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<CartResponseDTO> merge(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                 @RequestParam String guestId,
                                                 @RequestBody @Valid List<MergeCartLineDTO> items) {
        return ok(cartService.mergeCart(buyer(authUser), unwrapGuestId(guestId), quantitiesOf(items)));
    }

    @DeleteMapping("/guest-cart")
    public ResponseEntity<Void> deleteGuestCart(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                @RequestParam String guestId) {
        cartService.deleteGuestCart(unwrapGuestId(guestId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<CartResponseDTO> bulkAdd(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                   @RequestBody List<String> items) {
        return ok(cartService.addItems(buyer(authUser), null, items));
    }

    /** The signed guest id a client presented, unwrapped — or a 403. See {@link GuestCartIds}. */
    private String unwrapGuestId(String presented) {
        return guestCartIds.verify(presented).orElseThrow(UnknownGuestCartException::new);
    }

    private static UserId buyer(AuthenticatedUser authUser) {
        return authUser.id();
    }

    /**
     * The merge payload, reduced to what the server will actually believe. Repeated lines for
     * one product are added together rather than the last one winning.
     */
    static Map<ProductId, Quantity> quantitiesOf(List<MergeCartLineDTO> items) {
        Map<ProductId, Quantity> quantities = new LinkedHashMap<>();
        for (MergeCartLineDTO item : items) {
            if (item == null || item.getProductId() == null) {
                continue;
            }
            quantities.merge(ProductId.of(item.getProductId()),
                    Quantity.of(item.getQuantity()), Quantity::plus);
        }
        return quantities;
    }

    private ResponseEntity<CartResponseDTO> ok(com.dominator.gearly.cart.domain.Cart cart) {
        return ResponseEntity.ok(cartMapper.toResponseDto(cart));
    }
}
