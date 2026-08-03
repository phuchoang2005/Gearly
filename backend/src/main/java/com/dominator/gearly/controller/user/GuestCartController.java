package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.CartResponseDTO;
import com.dominator.gearly.mapper.CartMapper;
import com.dominator.gearly.model.CartItem;
import com.dominator.gearly.service.user.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/guest-cart")
@RequiredArgsConstructor
public class GuestCartController {
    private final CartService cartService;
    private final CartMapper cartMapper;

    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> init() {
        String guestId = UUID.randomUUID().toString();
        cartService.getOrCreate(null, guestId);
        return ResponseEntity.ok(Map.of("guestId", guestId));
    }

    @GetMapping
    public ResponseEntity<CartResponseDTO> get(@RequestParam String guestId) {
        return ResponseEntity.ok(cartMapper.toResponseDto(cartService.getOrCreate(null, guestId)));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> add(@RequestParam String guestId, @RequestBody CartItem item) {
        return ResponseEntity.ok(cartMapper.toResponseDto(cartService.addItem(null, guestId, item)));
    }

    @PutMapping("/update")
    public ResponseEntity<CartResponseDTO> update(@RequestParam String guestId,
                                       @RequestParam String productId,
                                       @RequestParam int quantity) {
        return ResponseEntity.ok(cartMapper.toResponseDto(
                cartService.updateQuantity(null, guestId, productId, quantity)));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> remove(@RequestParam String guestId,
                                       @RequestParam String productId) {
        cartService.removeItem(null, guestId, productId);
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
        return ResponseEntity.ok(cartMapper.toResponseDto(cartService.addItems(null, guestId, itemIds)));
    }
}
