package com.dominator.gearly.cart.api;

import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.cart.domain.CartLine;
import com.dominator.gearly.cart.domain.CartRepository;
import com.dominator.gearly.catalog.domain.Image;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>🔒 The S11 security verification, end to end.</b> "Posting a tampered {@code price} in an
 * add-to-cart body has no effect on what is stored."
 *
 * <p>Asserted through the real HTTP stack against a real MongoDB, not through a service call,
 * because every layer between the two is part of the claim: Jackson has to ignore the extra
 * fields rather than reject the request, the controller has to bind the shrunken DTO, the
 * aggregate has to hydrate from the catalog, and the converter has to store what it hydrated.
 * A unit test on any one of those would leave the others unproven.
 *
 * <p>{@code /api/guest-cart/**} is {@code permitAll}, so the attack needs no account — which
 * is what made this worth fixing first, and is also why no authentication setup appears below.
 *
 * <p>Docker-gated so {@code mvn test} still passes offline; run with Colima up to exercise it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CartPriceTamperingIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Autowired private MockMvc mvc;
    @Autowired private ProductRepository products;
    @Autowired private CartRepository carts;
    @Autowired private MongoTemplate mongoTemplate;

    private static final String GUEST_ID = "guest-under-test";

    /** What the storefront actually posts, with the price replaced by a penny. */
    private static final String TAMPERED_BODY = """
            {"productId":"%s","title":"Free GPU","author":"me","price":0.01,
             "quantity":1,"image":"http://evil/x.png","condition":"USED","stock":9999}
            """;

    private String productId;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Cart.class);
        mongoTemplate.dropCollection(Product.class);

        Product gpu = products.save(Product.create(
                "RTX 4090", List.of("NVIDIA"), "flagship GPU",
                Money.of(1599.00), Money.of(1799.00), ProductCondition.NEW, Quantity.of(5),
                null, List.of(new Image("http://img/gpu.png", "gpu"))));
        productId = gpu.getId();
    }

    @Test
    @DisplayName("a tampered price in the add-to-cart body is ignored — the catalog's price is stored")
    void tamperedPriceHasNoEffectOnWhatIsStored() throws Exception {
        mvc.perform(post("/api/guest-cart/add")
                        .param("guestId", GUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TAMPERED_BODY.formatted(productId)))
                .andExpect(status().isOk())
                // not even the response echoes the submitted values back
                .andExpect(jsonPath("$.items[0].price").value(1599.00))
                .andExpect(jsonPath("$.items[0].title").value("RTX 4090"));

        CartLine stored = carts.findByGuest(GUEST_ID).orElseThrow().getItems().getFirst();

        assertThat(stored.getPrice()).as("the catalog's price, not the caller's")
                .isEqualTo(Money.of(1599.00));
        assertThat(stored.getTitle()).isEqualTo("RTX 4090");
        assertThat(stored.getAuthor()).isEqualTo("NVIDIA");
        assertThat(stored.getImage()).isEqualTo("http://img/gpu.png");
        assertThat(stored.getCondition()).isEqualTo(ProductCondition.NEW);
        assertThat(stored.getStock()).as("the catalog's stock, not the claimed 9999")
                .isEqualTo(Quantity.of(5));
        assertThat(stored.getQuantity()).as("the one field the client does get to choose")
                .isEqualTo(Quantity.ONE);
    }

    /**
     * The compatibility half. The claim "no frontend change is required" is only true if the
     * five now-unknown properties are ignored rather than rejected, and the test above would
     * pass just as happily against a body that had already been trimmed.
     */
    @Test
    @DisplayName("the storefront's full eight-field body is still accepted, not rejected as malformed")
    void theStorefrontsExistingBodyStillWorks() throws Exception {
        mvc.perform(post("/api/guest-cart/add")
                        .param("guestId", GUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TAMPERED_BODY.formatted(productId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a body naming a product that does not exist is a 404, not a stored line")
    void unknownProductIsRejected() throws Exception {
        mvc.perform(post("/api/guest-cart/add")
                        .param("guestId", GUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TAMPERED_BODY.formatted("507f1f77bcf86cd799439011")))
                .andExpect(status().isNotFound());

        assertThat(carts.findByGuest(GUEST_ID))
                .hasValueSatisfying(cart -> assertThat(cart.getItems()).isEmpty());
    }
}
