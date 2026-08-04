package com.dominator.gearly.service.user;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.mapper.CartMapper;
import com.dominator.gearly.model.Cart;
import com.dominator.gearly.model.CartItem;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.repository.CartRepository;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <b>Characterization suite (S8).</b> Locks the <i>current</i> behavior of
 * {@link CartService} — bugs included — ahead of the S11 rewrite into a {@code Cart}
 * aggregate. Tests that pin something the DDD plan calls out as wrong say so in their
 * name and comment; those are the only assertions a later sprint may edit, and only in
 * the commit that makes the deliberate change.
 *
 * <p>The real {@link CartMapper} is used because the snapshot it builds (title, price,
 * image, condition, stock) is part of the behavior being characterized.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductService productService;

    private CartService service;

    private static final String USER_ID = "user-1";
    private static final String GUEST_ID = "guest-1";

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, productService, new CartMapper());
    }

    // ---- fixtures ----------------------------------------------------------

    private Product product(String id, double price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setTitle("Product " + id);
        p.setAuthors(List.of("Author " + id));
        p.setPrice(Money.of(price));
        p.setStock(stock);
        p.setCondition(ProductCondition.NEW);
        p.setImages(List.of(new Image("http://img/" + id + ".png", "alt")));
        return p;
    }

    private CartItem cartItem(String productId, int quantity, int stock) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setTitle("Product " + productId);
        item.setPrice(Money.of(10.00));
        item.setQuantity(quantity);
        item.setStock(stock);
        return item;
    }

    private Cart userCart(CartItem... items) {
        Cart cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId(USER_ID);
        cart.setItems(new ArrayList<>(List.of(items)));
        cart.setCreatedAt(new Date());
        cart.setUpdatedAt(new Date());
        return cart;
    }

    private void stubSaveReturnsArgument() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- getOrCreate + syncCartWithStock -----------------------------------

    @Nested
    @DisplayName("getOrCreate / syncCartWithStock")
    class GetOrCreate {

        @Test
        @DisplayName("a user with no cart gets a new empty one, persisted immediately")
        void newUserCartIsCreatedAndSaved() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            Cart cart = service.getOrCreate(USER_ID, null);

            assertThat(cart.getUserId()).isEqualTo(USER_ID);
            assertThat(cart.getGuestId()).isNull();
            assertThat(cart.getItems()).isEmpty();
            assertThat(cart.getCreatedAt()).isNotNull();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("a null userId routes the lookup to the guest cart")
        void nullUserIdUsesGuestLookup() {
            when(cartRepository.findByGuestId(GUEST_ID)).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            Cart cart = service.getOrCreate(null, GUEST_ID);

            assertThat(cart.getGuestId()).isEqualTo(GUEST_ID);
            verify(cartRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("an unchanged cart is not re-saved by the stock sync")
        void inSyncCartIsNotSaved() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));

            service.getOrCreate(USER_ID, null);

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("a line whose product no longer exists is dropped and the cart re-saved")
        void deletedProductLineIsDropped() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(null);

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("a line for an out-of-stock product is dropped entirely")
        void outOfStockLineIsDropped() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 0));

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("a line above available stock is clamped down, and its stock snapshot refreshed")
        void overstockedLineIsClamped() {
            Cart cart = userCart(cartItem("p1", 9, 9));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 3));

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getQuantity()).isEqualTo(3);
                assertThat(item.getStock()).isEqualTo(3);
            });
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("the stock snapshot on an in-range line is NOT refreshed — only clamped lines are")
        void inRangeLineKeepsStaleStockSnapshot() {
            // KNOWN INCONSISTENCY: `stock` on the line is only rewritten when the line has to be
            // clamped, so a cart that is still valid keeps whatever stock figure it was created
            // with. S11's reconcileWith(...) should refresh every line.
            Cart cart = userCart(cartItem("p1", 1, 99));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 4));

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getStock)
                    .isEqualTo(99);
        }
    }

    // ---- addItem -----------------------------------------------------------

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("a new line is appended verbatim from the request")
        void newLineIsAppended() {
            // KNOWN BUG (S11 price tampering): the CartItem comes straight off the request body,
            // so title/price/stock/condition are client-controlled and stored unverified.
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getStock("p1")).thenReturn(5);
            stubSaveReturnsArgument();

            CartItem incoming = cartItem("p1", 2, 5);
            incoming.setPrice(Money.of(0.01));

            Cart result = service.addItem(USER_ID, null, incoming);

            assertThat(result.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getQuantity()).isEqualTo(2);
                assertThat(item.getPrice()).isEqualTo(Money.of(0.01));
            });
        }

        @Test
        @DisplayName("adding a product already in the cart sums the quantities on the existing line")
        void existingLineAccumulates() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
            when(productService.getStock("p1")).thenReturn(5);
            stubSaveReturnsArgument();

            Cart result = service.addItem(USER_ID, null, cartItem("p1", 3, 5));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(5);
        }

        @Test
        @DisplayName("exceeding stock is rejected with the remaining-count message")
        void overStockIsRejected() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
            when(productService.getStock("p1")).thenReturn(5);

            assertThatThrownBy(() -> service.addItem(USER_ID, null, cartItem("p1", 4, 5)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only 5 Left!");

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("a zero-stock product is rejected with the out-of-stock message")
        void zeroStockUsesOutOfStockMessage() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getStock("p1")).thenReturn(0);

            assertThatThrownBy(() -> service.addItem(USER_ID, null, cartItem("p1", 1, 0)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("This item is out of stock!");
        }

        @Test
        @DisplayName("a null item is rejected before any repository work")
        void nullItemIsRejected() {
            assertThatThrownBy(() -> service.addItem(USER_ID, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("CartItem must not be null");

            verify(cartRepository, never()).save(any());
        }
    }

    // ---- updateQuantity ----------------------------------------------------

    @Nested
    @DisplayName("updateQuantity")
    class UpdateQuantity {

        @Test
        @DisplayName("sets the line quantity outright rather than accumulating")
        void setsAbsoluteQuantity() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(productService.getStock("p1")).thenReturn(5);
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
            stubSaveReturnsArgument();

            Cart result = service.updateQuantity(USER_ID, null, "p1", 4);

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("the stock check runs before the cart is even loaded")
        void stockIsCheckedBeforeLoadingTheCart() {
            when(productService.getStock("p1")).thenReturn(2);

            assertThatThrownBy(() -> service.updateQuantity(USER_ID, null, "p1", 3))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only 2 Left!");

            verify(cartRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("updating a product that is not in the cart silently does nothing, but still saves")
        void unknownProductIsANoOpThatStillSaves() {
            Cart cart = userCart();
            when(productService.getStock("p1")).thenReturn(5);
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            stubSaveReturnsArgument();

            Cart result = service.updateQuantity(USER_ID, null, "p1", 3);

            assertThat(result.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("a quantity of zero is accepted and leaves a zero-quantity line behind")
        void zeroQuantityLeavesAnEmptyLine() {
            // KNOWN BUG: there is no lower bound, so a line can sit in the cart with quantity 0
            // (or negative). S11's Quantity VO / changeQuantity must reject it or remove the line.
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(productService.getStock("p1")).thenReturn(5);
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
            stubSaveReturnsArgument();

            Cart result = service.updateQuantity(USER_ID, null, "p1", 0);

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(0);
        }
    }

    // ---- removeItem / removeItems / clearCart ------------------------------

    @Nested
    @DisplayName("removal")
    class Removal {

        @Test
        @DisplayName("removeItem drops the matching line and keeps the others")
        void removeItemDropsOneLine() {
            Cart cart = userCart(cartItem("p1", 2, 5), cartItem("p2", 1, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
            when(productService.getProductById("p2")).thenReturn(product("p2", 10.00, 5));
            stubSaveReturnsArgument();

            service.removeItem(USER_ID, null, "p1");

            assertThat(cart.getItems()).extracting(CartItem::getProductId).containsExactly("p2");
        }

        @Test
        @DisplayName("removeItems decrements a line when fewer units are removed than it holds")
        void removeItemsDecrementsPartially() {
            Cart cart = userCart(cartItem("p1", 5, 10));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 10));
            stubSaveReturnsArgument();

            service.removeItems(USER_ID, null, Map.of("p1", 2));

            assertThat(cart.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("removeItems drops the line when the removed quantity meets or exceeds it")
        void removeItemsDropsLineWhenFullyConsumed() {
            Cart cart = userCart(cartItem("p1", 2, 10), cartItem("p2", 1, 10));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 10));
            when(productService.getProductById("p2")).thenReturn(product("p2", 10.00, 10));
            stubSaveReturnsArgument();

            service.removeItems(USER_ID, null, Map.of("p1", 2));

            assertThat(cart.getItems()).extracting(CartItem::getProductId).containsExactly("p2");
        }

        @Test
        @DisplayName("removeItems with an empty map is a complete no-op — the cart is not even loaded")
        void removeItemsWithEmptyMapDoesNothing() {
            service.removeItems(USER_ID, null, Map.of());

            verify(cartRepository, never()).findByUserId(any());
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("clearCart empties the line list and saves")
        void clearCartEmptiesTheCart() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
            stubSaveReturnsArgument();

            service.clearCart(USER_ID, null);

            assertThat(cart.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("deleteGuestCart delegates straight to the repository")
        void deleteGuestCartDelegates() {
            service.deleteGuestCart(GUEST_ID);

            verify(cartRepository).deleteByGuestId(GUEST_ID);
        }
    }

    // ---- mergeCart ---------------------------------------------------------

    @Nested
    @DisplayName("mergeCart")
    class MergeCart {

        @Test
        @DisplayName("a guest line for a product not yet in the user cart is added, clamped to stock")
        void newLineIsAddedClampedToStock() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getStock("p1")).thenReturn(3);
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID, List.of(cartItem("p1", 10, 10)));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(3);
            verify(cartRepository).deleteByGuestId(GUEST_ID);
        }

        @Test
        @DisplayName("quantities for a product already in the user cart are summed, then clamped to stock")
        void existingLineIsSummedThenClamped() {
            Cart cart = userCart(cartItem("p1", 2, 5));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 4));
            when(productService.getStock("p1")).thenReturn(4);
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID, List.of(cartItem("p1", 3, 5)));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("KNOWN BUG: an out-of-stock guest line is merged in with quantity 0 rather than dropped")
        void outOfStockLineIsMergedWithQuantityZero() {
            // min(incoming, 0) == 0, and there is no guard, so the cart gains a dead line.
            // S11's Cart.merge must skip zero-stock lines.
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getStock("p1")).thenReturn(0);
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID, List.of(cartItem("p1", 2, 0)));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("null entries in the incoming list are skipped")
        void nullIncomingLinesAreSkipped() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            stubSaveReturnsArgument();

            List<CartItem> incoming = new ArrayList<>();
            incoming.add(null);

            Cart result = service.mergeCart(USER_ID, GUEST_ID, incoming);

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("the merge always targets the user cart, never the guest cart")
        void mergeAlwaysTargetsTheUserCart() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            stubSaveReturnsArgument();

            service.mergeCart(USER_ID, GUEST_ID, List.of());

            verify(cartRepository, never()).findByGuestId(any());
            verify(cartRepository).deleteByGuestId(GUEST_ID);
        }
    }

    // ---- addItems ----------------------------------------------------------

    @Nested
    @DisplayName("addItems")
    class AddItems {

        @Test
        @DisplayName("each id adds one unit, hydrated from the catalog rather than the request")
        void addsOneUnitPerIdFromTheCatalog() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 24.99, 5));
            stubSaveReturnsArgument();

            Cart result = service.addItems(USER_ID, null, List.of("p1"));

            assertThat(result.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getProductId()).isEqualTo("p1");
                assertThat(item.getTitle()).isEqualTo("Product p1");
                assertThat(item.getAuthor()).isEqualTo("Author p1");
                assertThat(item.getPrice()).isEqualTo(Money.of(24.99));
                assertThat(item.getQuantity()).isEqualTo(1);
                assertThat(item.getImage()).isEqualTo("http://img/p1.png");
                assertThat(item.getCondition()).isEqualTo(ProductCondition.NEW);
                assertThat(item.getStock()).isEqualTo(5);
            });
        }

        @Test
        @DisplayName("the same id twice increments the existing line to 2")
        void repeatedIdIncrementsTheLine() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
            stubSaveReturnsArgument();

            Cart result = service.addItems(USER_ID, null, List.of("p1", "p1"));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartItem::getQuantity)
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("an unknown product id is a 404")
        void unknownProductThrows() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("nope")).thenReturn(null);

            assertThatThrownBy(() -> service.addItems(USER_ID, null, List.of("nope")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found: nope");
        }

        @Test
        @DisplayName("KNOWN BUG: the over-stock message is truncated — it names no product and no count")
        void overStockMessageIsTruncated() {
            // "Only N Left for " with nothing appended. S11 should build a complete message.
            Cart cart = userCart(cartItem("p1", 2, 2));
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 2));

            assertThatThrownBy(() -> service.addItems(USER_ID, null, List.of("p1")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Only 2 Left for ");
        }

        @Test
        @DisplayName("a zero-stock product is reported by title — though the sync already dropped the line")
        void zeroStockIsReportedByTitle() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 0));

            assertThatThrownBy(() -> service.addItems(USER_ID, null, List.of("p1")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("\"Product p1\" is out of stock!");
        }

        @Test
        @DisplayName("null ids in the list are skipped")
        void nullIdsAreSkipped() {
            Cart cart = userCart();
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            stubSaveReturnsArgument();

            List<String> ids = new ArrayList<>();
            ids.add(null);

            Cart result = service.addItems(USER_ID, null, ids);

            assertThat(result.getItems()).isEmpty();
        }
    }

    // ---- cross-cutting -----------------------------------------------------

    @Test
    @DisplayName("every write path refreshes updatedAt")
    void writePathsTouchUpdatedAt() {
        Cart cart = userCart(cartItem("p1", 1, 5));
        cart.setUpdatedAt(new Date(0L));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 5));
        stubSaveReturnsArgument();

        service.clearCart(USER_ID, null);

        assertThat(cart.getUpdatedAt()).isAfter(new Date(0L));
    }

    @Test
    @DisplayName("a stock-clamping sync followed by a write saves the cart twice")
    void syncAndWriteEachSave() {
        // The sync saves, then the operation saves again — two round trips for one call.
        // Worth knowing before S11 collapses these into one aggregate write.
        Cart cart = userCart(cartItem("p1", 9, 9));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 3));
        stubSaveReturnsArgument();

        service.removeItem(USER_ID, null, "p2");

        verify(cartRepository, times(2)).save(cart);
    }
}
