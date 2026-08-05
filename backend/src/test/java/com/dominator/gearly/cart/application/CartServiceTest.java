package com.dominator.gearly.cart.application;

import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.cart.domain.CartFixture;
import com.dominator.gearly.cart.domain.CartLine;
import com.dominator.gearly.cart.domain.CartRepository;
import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.catalog.domain.InsufficientStockException;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <b>The S8 characterization suite, carried forward onto the aggregate.</b> Same coverage as
 * {@code service.user.CartServiceTest} — get / add / update / merge / remove / reconcile — with
 * every assertion that pinned a {@code KNOWN BUG} now pinning the fix instead, and saying so.
 *
 * <p>The mocks changed shape, which is itself the point of the sprint: this used to mock a
 * {@code ProductService} that handed over whole {@code Product} aggregates. It mocks
 * {@link ProductSnapshotPort} now, so the cart is tested against exactly what the catalog
 * publishes and nothing more.
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductSnapshotPort catalog;

    private CartService service;

    private static final UserId USER_ID = UserId.of("user-1");
    private static final String GUEST_ID = "guest-1";

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, catalog);
    }

    // ---- fixtures ----------------------------------------------------------

    private CatalogSnapshot snapshot(String id, double price, int stock) {
        return CartFixture.snapshot(id, price, stock);
    }

    private void catalogHolds(String id, double price, int stock) {
        CatalogSnapshot snapshot = snapshot(id, price, stock);
        when(catalog.snapshotsOf(anyList())).thenReturn(List.of(snapshot));
        when(catalog.snapshotOf(ProductId.of(id))).thenReturn(snapshot);
    }

    private void catalogListsOnly(CatalogSnapshot... snapshots) {
        when(catalog.snapshotsOf(anyList())).thenReturn(List.of(snapshots));
    }

    private Cart userCartHolding(String productId, int quantity, int stockWhenAdded) {
        return CartFixture.aCart().ownedBy(USER_ID.value())
                .holding(productId, 10.00, stockWhenAdded, quantity)
                .build();
    }

    private void userCartIs(Cart cart) {
        when(cartRepository.findByUser(USER_ID)).thenReturn(Optional.of(cart));
    }

    private void stubSaveReturnsArgument() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- getOrCreate + reconcile -------------------------------------------

    @Nested
    @DisplayName("getOrCreate / reconcileWith")
    class GetOrCreate {

        @Test
        @DisplayName("a user with no cart gets a new empty one, persisted immediately")
        void newUserCartIsCreatedAndSaved() {
            when(cartRepository.findByUser(USER_ID)).thenReturn(Optional.empty());
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
            when(cartRepository.findByGuest(GUEST_ID)).thenReturn(Optional.empty());
            stubSaveReturnsArgument();

            Cart cart = service.getOrCreate(null, GUEST_ID);

            assertThat(cart.getGuestId()).isEqualTo(GUEST_ID);
            verify(cartRepository, never()).findByUser(any());
        }

        @Test
        @DisplayName("an empty cart is not reconciled at all — no catalog read, no save")
        void emptyCartCostsNothing() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());

            service.getOrCreate(USER_ID, null);

            verify(catalog, never()).snapshotsOf(anyList());
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("an unchanged cart is not re-saved by the reconcile")
        void inSyncCartIsNotSaved() {
            userCartIs(userCartHolding("p1", 2, 5));
            catalogListsOnly(snapshot("p1", 10.00, 5));

            service.getOrCreate(USER_ID, null);

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("a line whose product no longer exists is dropped and the cart re-saved")
        void deletedProductLineIsDropped() {
            Cart cart = userCartHolding("p1", 2, 5);
            userCartIs(cart);
            catalogListsOnly();   // the product is absent from the catalog's answer
            stubSaveReturnsArgument();

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("a line for an out-of-stock product is dropped entirely")
        void outOfStockLineIsDropped() {
            Cart cart = userCartHolding("p1", 2, 5);
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 0));
            stubSaveReturnsArgument();

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("a line above available stock is clamped down, and its stock hint refreshed")
        void overstockedLineIsClamped() {
            Cart cart = userCartHolding("p1", 9, 9);
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 3));
            stubSaveReturnsArgument();

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).singleElement().satisfies(line -> {
                assertThat(line.getQuantity()).isEqualTo(Quantity.of(3));
                assertThat(line.getStock()).isEqualTo(Quantity.of(3));
            });
            verify(cartRepository).save(cart);
        }

        /**
         * <b>FIXED — was a KNOWN INCONSISTENCY.</b> The old sweep only rewrote a line's
         * {@code stock} when it also had to clamp the quantity, so a cart that was still valid
         * kept whatever stock figure it happened to be created with. The storefront caps its
         * quantity stepper with that number, so it was capping against a months-stale figure.
         * The S8 assertion said "S11's reconcileWith(...) should refresh every line"; it does.
         */
        @Test
        @DisplayName("FIXED (was a KNOWN INCONSISTENCY): an in-range line's stock hint is refreshed too")
        void inRangeLineGetsAFreshStockHint() {
            Cart cart = userCartHolding("p1", 1, 99);
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 4));
            stubSaveReturnsArgument();

            Cart result = service.getOrCreate(USER_ID, null);

            assertThat(result.getItems()).singleElement()
                    .extracting(CartLine::getStock)
                    .isEqualTo(Quantity.of(4));
        }

        /**
         * <b>FIXED.</b> The old code saved in {@code syncCartWithStock} and then again in the
         * operation that followed — two round trips for one request, which the S8 suite pinned
         * as a fact worth knowing before this rewrite.
         */
        @Test
        @DisplayName("FIXED: a reconcile followed by a write saves the cart once, not twice")
        void reconcileAndWriteSaveOnce() {
            Cart cart = userCartHolding("p1", 9, 9);
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 3));
            stubSaveReturnsArgument();

            service.removeItem(USER_ID, null, ProductId.of("p2"));

            verify(cartRepository, times(1)).save(cart);
        }

        @Test
        @DisplayName("the whole basket is read from the catalog in one call, not one per line")
        void reconcileMakesOneCatalogRead() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value())
                    .holding("p1", 10.00, 5, 1)
                    .holding("p2", 20.00, 5, 1)
                    .build());
            catalogListsOnly(snapshot("p1", 10.00, 5), snapshot("p2", 20.00, 5));

            service.getOrCreate(USER_ID, null);

            verify(catalog, times(1)).snapshotsOf(anyList());
        }
    }

    // ---- addItem -----------------------------------------------------------

    @Nested
    @DisplayName("addItem")
    class AddItem {

        /**
         * <b>🔒 FIXED — was the KNOWN BUG "S11 price tampering".</b> The old endpoint bound
         * {@code CartItem} off the request body, so a client-supplied {@code price} of $0.01
         * was persisted verbatim against a $1,599 product. There is no request-supplied price
         * any more: a line can only be built from a {@link CatalogSnapshot}.
         */
        @Test
        @DisplayName("FIXED (price tampering): a new line takes every field from the catalog")
        void newLineIsHydratedFromTheCatalog() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotOf(ProductId.of("p1"))).thenReturn(snapshot("p1", 1599.00, 5));
            stubSaveReturnsArgument();

            Cart result = service.addItem(USER_ID, null, ProductId.of("p1"), Quantity.of(2));

            assertThat(result.getItems()).singleElement().satisfies(line -> {
                assertThat(line.getQuantity()).isEqualTo(Quantity.of(2));
                assertThat(line.getPrice()).as("the catalog's price, not the caller's")
                        .isEqualTo(Money.of(1599.00));
                assertThat(line.getTitle()).isEqualTo("Product p1");
                assertThat(line.getAuthor()).isEqualTo("Author p1");
                assertThat(line.getImage()).isEqualTo("http://img/p1.png");
                assertThat(line.getStock()).isEqualTo(Quantity.of(5));
            });
        }

        @Test
        @DisplayName("adding a product already in the cart sums the quantities on the existing line")
        void existingLineAccumulates() {
            userCartIs(userCartHolding("p1", 2, 5));
            catalogHolds("p1", 10.00, 5);
            stubSaveReturnsArgument();

            Cart result = service.addItem(USER_ID, null, ProductId.of("p1"), Quantity.of(3));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartLine::getQuantity)
                    .isEqualTo(Quantity.of(5));
        }

        /**
         * The message is the unified one now. It used to be {@code "Only 5 Left!"} here and
         * {@code "Only 5 Left for "} — truncated, naming nothing — from {@code addItems}, two
         * of the five hand-written copies of this check.
         */
        @Test
        @DisplayName("exceeding stock is rejected, naming the product and the remaining count")
        void overStockIsRejected() {
            userCartIs(userCartHolding("p1", 2, 5));
            catalogHolds("p1", 10.00, 5);

            assertThatThrownBy(() -> service.addItem(USER_ID, null, ProductId.of("p1"), Quantity.of(4)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Only 5 left for \"Product p1\"!");

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("a zero-stock product is rejected with the out-of-stock message")
        void zeroStockUsesOutOfStockMessage() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotOf(ProductId.of("p1"))).thenReturn(snapshot("p1", 10.00, 0));

            assertThatThrownBy(() -> service.addItem(USER_ID, null, ProductId.of("p1"), Quantity.ONE))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("\"Product p1\" is out of stock!");
        }

        @Test
        @DisplayName("adding a product the catalog does not have is a 404")
        void unknownProductIsNotFound() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotOf(ProductId.of("nope")))
                    .thenThrow(new ProductNotFoundException(ProductId.of("nope")));

            assertThatThrownBy(() -> service.addItem(USER_ID, null, ProductId.of("nope"), Quantity.ONE))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ---- updateQuantity ----------------------------------------------------

    @Nested
    @DisplayName("updateQuantity")
    class UpdateQuantity {

        @Test
        @DisplayName("sets the line quantity outright rather than accumulating")
        void setsAbsoluteQuantity() {
            userCartIs(userCartHolding("p1", 2, 5));
            catalogHolds("p1", 10.00, 5);
            stubSaveReturnsArgument();

            Cart result = service.updateQuantity(USER_ID, null, ProductId.of("p1"), 4);

            assertThat(result.getItems()).singleElement()
                    .extracting(CartLine::getQuantity)
                    .isEqualTo(Quantity.of(4));
        }

        @Test
        @DisplayName("asking for more than exists is rejected and the line is untouched")
        void overStockIsRejected() {
            Cart cart = userCartHolding("p1", 2, 5);
            userCartIs(cart);
            catalogHolds("p1", 10.00, 2);

            assertThatThrownBy(() -> service.updateQuantity(USER_ID, null, ProductId.of("p1"), 3))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Only 2 left for \"Product p1\"!");

            assertThat(cart.getItems()).singleElement()
                    .extracting(CartLine::getQuantity)
                    .isEqualTo(Quantity.of(2));
        }

        @Test
        @DisplayName("updating a product that is not in the cart silently does nothing, but still saves")
        void unknownProductIsANoOpThatStillSaves() {
            Cart cart = CartFixture.aCart().ownedBy(USER_ID.value()).build();
            userCartIs(cart);
            when(catalog.snapshotOf(ProductId.of("p1"))).thenReturn(snapshot("p1", 10.00, 5));
            stubSaveReturnsArgument();

            Cart result = service.updateQuantity(USER_ID, null, ProductId.of("p1"), 3);

            assertThat(result.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        /**
         * <b>FIXED — was a KNOWN BUG.</b> Nothing bounded the quantity below, so
         * {@code updateQuantity(…, 0)} left a dead line sitting in the cart. The S8 assertion
         * said "S11's Quantity VO / changeQuantity must reject it or remove the line"; it
         * removes the line, which is what the storefront's stepper means by decrementing to
         * zero.
         */
        @Test
        @DisplayName("FIXED (was a KNOWN BUG): a quantity of zero removes the line")
        void zeroQuantityRemovesTheLine() {
            userCartIs(userCartHolding("p1", 2, 5));
            catalogHolds("p1", 10.00, 5);
            stubSaveReturnsArgument();

            Cart result = service.updateQuantity(USER_ID, null, ProductId.of("p1"), 0);

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("a negative quantity is refused outright")
        void negativeQuantityIsRefused() {
            userCartIs(userCartHolding("p1", 2, 5));
            catalogHolds("p1", 10.00, 5);

            assertThatThrownBy(() -> service.updateQuantity(USER_ID, null, ProductId.of("p1"), -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ---- removeItem / removeItems / clearCart ------------------------------

    @Nested
    @DisplayName("removal")
    class Removal {

        @Test
        @DisplayName("removeItem drops the matching line and keeps the others")
        void removeItemDropsOneLine() {
            Cart cart = CartFixture.aCart().ownedBy(USER_ID.value())
                    .holding("p1", 10.00, 5, 2)
                    .holding("p2", 10.00, 5, 1)
                    .build();
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 5), snapshot("p2", 10.00, 5));
            stubSaveReturnsArgument();

            service.removeItem(USER_ID, null, ProductId.of("p1"));

            assertThat(cart.getItems()).extracting(CartLine::getProductId)
                    .containsExactly(ProductId.of("p2"));
        }

        @Test
        @DisplayName("removeItems decrements a line when fewer units are removed than it holds")
        void removeItemsDecrementsPartially() {
            Cart cart = userCartHolding("p1", 5, 10);
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 10));
            stubSaveReturnsArgument();

            service.removeItems(USER_ID, null, Map.of(ProductId.of("p1"), Quantity.of(2)));

            assertThat(cart.getItems()).singleElement()
                    .extracting(CartLine::getQuantity)
                    .isEqualTo(Quantity.of(3));
        }

        @Test
        @DisplayName("removeItems drops the line when the removed quantity meets or exceeds it")
        void removeItemsDropsLineWhenFullyConsumed() {
            Cart cart = CartFixture.aCart().ownedBy(USER_ID.value())
                    .holding("p1", 10.00, 10, 2)
                    .holding("p2", 10.00, 10, 1)
                    .build();
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 10), snapshot("p2", 10.00, 10));
            stubSaveReturnsArgument();

            service.removeItems(USER_ID, null, Map.of(ProductId.of("p1"), Quantity.of(2)));

            assertThat(cart.getItems()).extracting(CartLine::getProductId)
                    .containsExactly(ProductId.of("p2"));
        }

        @Test
        @DisplayName("removeItems with an empty map is a complete no-op — the cart is not even loaded")
        void removeItemsWithEmptyMapDoesNothing() {
            service.removeItems(USER_ID, null, Map.of());

            verify(cartRepository, never()).findByUser(any());
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("clearCart empties the line list and saves")
        void clearCartEmptiesTheCart() {
            Cart cart = userCartHolding("p1", 2, 5);
            userCartIs(cart);
            catalogListsOnly(snapshot("p1", 10.00, 5));
            stubSaveReturnsArgument();

            service.clearCart(USER_ID, null);

            assertThat(cart.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("deleteGuestCart delegates straight to the repository")
        void deleteGuestCartDelegates() {
            service.deleteGuestCart(GUEST_ID);

            verify(cartRepository).deleteByGuest(GUEST_ID);
        }
    }

    // ---- mergeCart ---------------------------------------------------------

    @Nested
    @DisplayName("mergeCart")
    class MergeCart {

        @Test
        @DisplayName("a guest line for a product not yet in the user cart is added, clamped to stock")
        void newLineIsAddedClampedToStock() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotsOf(anyList())).thenReturn(List.of(snapshot("p1", 10.00, 3)));
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID,
                    Map.of(ProductId.of("p1"), Quantity.of(10)));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartLine::getQuantity)
                    .isEqualTo(Quantity.of(3));
            verify(cartRepository).deleteByGuest(GUEST_ID);
        }

        @Test
        @DisplayName("quantities for a product already in the user cart are summed, then clamped to stock")
        void existingLineIsSummedThenClamped() {
            userCartIs(userCartHolding("p1", 2, 5));
            when(catalog.snapshotsOf(anyList())).thenReturn(List.of(snapshot("p1", 10.00, 4)));
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID,
                    Map.of(ProductId.of("p1"), Quantity.of(3)));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartLine::getQuantity)
                    .isEqualTo(Quantity.of(4));
        }

        /**
         * <b>FIXED — was a KNOWN BUG.</b> {@code Math.min(incoming, 0)} with no guard merged an
         * out-of-stock product in at quantity zero, leaving a dead line in the cart. The S8
         * assertion said "S11's Cart.merge must skip zero-stock lines"; it does.
         */
        @Test
        @DisplayName("FIXED (was a KNOWN BUG): an out-of-stock guest line is skipped, not merged at zero")
        void outOfStockLineIsSkipped() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotsOf(anyList())).thenReturn(List.of(snapshot("p1", 10.00, 0)));
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID,
                    Map.of(ProductId.of("p1"), Quantity.of(2)));

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("a guest line whose product has been delisted is skipped")
        void delistedProductIsSkipped() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotsOf(anyList())).thenReturn(List.of());
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID,
                    Map.of(ProductId.of("gone"), Quantity.of(2)));

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("the merge always targets the user cart, never the guest cart")
        void mergeAlwaysTargetsTheUserCart() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            stubSaveReturnsArgument();

            service.mergeCart(USER_ID, GUEST_ID, Map.of());

            verify(cartRepository, never()).findByGuest(any());
            verify(cartRepository).deleteByGuest(GUEST_ID);
        }

        /**
         * <b>🔒</b> The merge endpoint was the third way to post a price the server believed.
         * The guest cart contributes quantities and nothing else now.
         */
        @Test
        @DisplayName("merged lines are priced by the catalog, not by the incoming payload")
        void mergedLinesArePricedByTheCatalog() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotsOf(anyList())).thenReturn(List.of(snapshot("p1", 1599.00, 5)));
            stubSaveReturnsArgument();

            Cart result = service.mergeCart(USER_ID, GUEST_ID,
                    Map.of(ProductId.of("p1"), Quantity.ONE));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartLine::getPrice)
                    .isEqualTo(Money.of(1599.00));
        }
    }

    // ---- addItems ----------------------------------------------------------

    @Nested
    @DisplayName("addItems")
    class AddItems {

        @Test
        @DisplayName("each id adds one unit, hydrated from the catalog")
        void addsOneUnitPerIdFromTheCatalog() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotOf(ProductId.of("p1"))).thenReturn(snapshot("p1", 24.99, 5));
            stubSaveReturnsArgument();

            Cart result = service.addItems(USER_ID, null, List.of("p1"));

            assertThat(result.getItems()).singleElement().satisfies(line -> {
                assertThat(line.getProductId()).isEqualTo(ProductId.of("p1"));
                assertThat(line.getTitle()).isEqualTo("Product p1");
                assertThat(line.getAuthor()).isEqualTo("Author p1");
                assertThat(line.getPrice()).isEqualTo(Money.of(24.99));
                assertThat(line.getQuantity()).isEqualTo(Quantity.ONE);
                assertThat(line.getImage()).isEqualTo("http://img/p1.png");
                assertThat(line.getStock()).isEqualTo(Quantity.of(5));
            });
        }

        @Test
        @DisplayName("the same id twice increments the existing line to 2")
        void repeatedIdIncrementsTheLine() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotOf(ProductId.of("p1"))).thenReturn(snapshot("p1", 10.00, 5));
            stubSaveReturnsArgument();

            Cart result = service.addItems(USER_ID, null, List.of("p1", "p1"));

            assertThat(result.getItems()).singleElement()
                    .extracting(CartLine::getQuantity)
                    .isEqualTo(Quantity.of(2));
        }

        @Test
        @DisplayName("an unknown product id is a 404")
        void unknownProductThrows() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotOf(ProductId.of("nope")))
                    .thenThrow(new ProductNotFoundException("Product not found: nope"));

            assertThatThrownBy(() -> service.addItems(USER_ID, null, List.of("nope")))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found: nope");
        }

        /**
         * <b>FIXED — was a KNOWN BUG.</b> The message was {@code "Only 2 Left for "}, with
         * nothing appended: it named neither the product nor anything after the preposition,
         * because this call site composed its own string and got it wrong. The exception
         * builds the message now, so there is only one to get right.
         */
        @Test
        @DisplayName("FIXED (was a KNOWN BUG): the over-stock message names the product")
        void overStockMessageIsComplete() {
            userCartIs(userCartHolding("p1", 2, 2));
            catalogHolds("p1", 10.00, 2);

            assertThatThrownBy(() -> service.addItems(USER_ID, null, List.of("p1")))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Only 2 left for \"Product p1\"!");
        }

        @Test
        @DisplayName("a zero-stock product is reported by title")
        void zeroStockIsReportedByTitle() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            when(catalog.snapshotOf(ProductId.of("p1"))).thenReturn(snapshot("p1", 10.00, 0));

            assertThatThrownBy(() -> service.addItems(USER_ID, null, List.of("p1")))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("\"Product p1\" is out of stock!");
        }

        @Test
        @DisplayName("null ids in the list are skipped")
        void nullIdsAreSkipped() {
            userCartIs(CartFixture.aCart().ownedBy(USER_ID.value()).build());
            stubSaveReturnsArgument();

            List<String> ids = new java.util.ArrayList<>();
            ids.add(null);

            Cart result = service.addItems(USER_ID, null, ids);

            assertThat(result.getItems()).isEmpty();
        }
    }

    // ---- cross-cutting -----------------------------------------------------

    @Test
    @DisplayName("every write path refreshes updatedAt")
    void writePathsTouchUpdatedAt() {
        Cart cart = CartFixture.aCart().ownedBy(USER_ID.value())
                .holding("p1", 10.00, 5, 1)
                .persistedAs("cart-1", Instant.EPOCH, Instant.EPOCH)
                .build();
        userCartIs(cart);
        catalogListsOnly(snapshot("p1", 10.00, 5));
        stubSaveReturnsArgument();

        service.clearCart(USER_ID, null);

        assertThat(cart.getUpdatedAt()).isAfter(Instant.EPOCH);
    }

    @Test
    @DisplayName("a cart belongs to a user xor a guest — never both, never neither")
    void ownershipIsExclusive() {
        // Unenforced before: newCart(userId, guestId) assigned whatever it was handed, so a
        // cart with both or with neither was representable and nothing would have noticed.
        assertThatThrownBy(() -> Cart.openFor(USER_ID, GUEST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("never both and never neither");
        assertThatThrownBy(() -> Cart.openFor(null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cart.openFor(null, "  "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(Cart.openFor(USER_ID, null).getUserId()).isEqualTo(USER_ID);
        assertThat(Cart.openFor(null, GUEST_ID).getGuestId()).isEqualTo(GUEST_ID);
    }
}
