package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.CartResponseDTO;
import com.dominator.gearly.dto.OrderResponseDTO;
import com.dominator.gearly.dto.ProductResponseDTO;
import com.dominator.gearly.model.Cart;
import com.dominator.gearly.model.CartItem;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.Payment;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.User;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the S7 "entity -> response DTO" change: each response DTO must serialize
 * to exactly the same JSON as the entity it replaced, so neither frontend sees a
 * wire change. Uses the app's auto-configured {@link ObjectMapper} and compares
 * the parsed JSON trees (order-insensitive).
 *
 * <h2>Why the DTO-equals-entity tests are not enough on their own (S9)</h2>
 * Those tests compare a DTO against the entity it mirrors. When S9 gave <em>both</em>
 * sides a {@code Money} instead of a {@code double}, they kept passing — and would have
 * kept passing even if {@code Money} had serialized as {@code {"amount":…}}. They pin the
 * two representations to <em>each other</em>, not to the format the frontends actually
 * parse.
 *
 * <p>{@link ValueObjectWireFormat} is the missing half: it asserts the literal JSON node
 * type and value for every field that changed Java type in S9. That is what makes "the
 * value objects are invisible on the wire" a checked claim rather than a hope.
 */
@JsonTest
class ResponseDtoWireCompatTest {

    @Autowired
    private ObjectMapper json;

    private final OrderMapper orderMapper = new OrderMapper();
    private final CartMapper cartMapper = new CartMapper();
    private final ProductMapper productMapper = new ProductMapper();

    @Test
    void orderResponseDto_matchesEntityWire() {
        Order order = new Order();
        order.setId("o1");
        order.setUserId("u1");
        order.setItems(List.of(new OrderItem("p1", "RTX 4090", Money.of(1599.0), "http://img/a.png", 2)));
        order.setTotalAmount(Money.of(3198.0));
        order.setPayment(new Payment("MOMO", List.of()));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setReviewed(true);
        order.setNote("leave at door");
        order.setAddedAt(Instant.parse("2026-01-02T03:04:05Z"));
        order.setModifiedAt(Instant.parse("2026-01-03T03:04:05Z"));
        order.setDoneAt(Instant.parse("2026-01-04T03:04:05Z"));

        OrderResponseDTO dto = orderMapper.toResponseDto(order);

        JsonNode dtoNode = json.valueToTree(dto);
        JsonNode entityNode = json.valueToTree(order);
        assertThat(dtoNode).isEqualTo(entityNode);
    }

    @Test
    void cartResponseDto_matchesEntityWire() {
        Cart cart = new Cart();
        cart.setId("c1");
        cart.setUserId("u1");
        cart.setGuestId(null);
        cart.setItems(List.of(new CartItem("p1", "RTX 4090", "NVIDIA", Money.of(1599.0), 1, "http://img/a.png", ProductCondition.NEW, 5)));
        cart.setCreatedAt(new Date(1_700_000_000_000L));
        cart.setUpdatedAt(new Date(1_700_000_100_000L));

        CartResponseDTO dto = cartMapper.toResponseDto(cart);

        JsonNode dtoNode = json.valueToTree(dto);
        JsonNode entityNode = json.valueToTree(cart);
        assertThat(dtoNode).isEqualTo(entityNode);
    }

    @Test
    void productResponseDto_matchesEntityWire() {
        Product product = new Product();
        product.setId("p1");
        product.setTitle("RTX 4090");
        product.setAuthors(List.of("NVIDIA"));
        product.setDescription("flagship GPU");
        product.setPrice(Money.of(1599.0));
        product.setOriginalPrice(Money.of(1799.0));
        product.setCondition(ProductCondition.NEW);
        product.setStock(5);
        product.setCategoryIds(List.of(CategoryId.of("64b7f0000000000000000001")));
        product.setImages(List.of(new Image("http://img/a.png", "gpu")));
        product.setCategoryNames(List.of("Graphics Cards"));
        product.setAverageRating(4.5);
        product.setRatingCount(10);
        product.setTotalRating(45);
        product.setAddedAt(Instant.parse("2026-01-02T03:04:05Z"));
        product.setModifiedAt(Instant.parse("2026-01-03T03:04:05Z"));

        ProductResponseDTO dto = productMapper.toResponseDto(product);

        JsonNode dtoNode = json.valueToTree(dto);
        JsonNode entityNode = json.valueToTree(product);
        assertThat(dtoNode).isEqualTo(entityNode);
    }

    /**
     * Pins the literal JSON produced for every field whose Java type changed in S9.
     *
     * <p>Each assertion checks the JSON <em>node type</em> as well as the value, because
     * that is exactly where a value object can leak: {@code Money} holding a
     * {@link java.math.BigDecimal} would serialize {@code 1599.00} as a
     * {@code DecimalNode} where the wire has {@code 1599.0} as a {@code DoubleNode} — same
     * number, different token, and a visible change to anything doing a string comparison
     * or an exact match on the response.
     */
    @Nested
    class ValueObjectWireFormat {

        @Test
        void moneySerializesAsTheDoubleItAlwaysWas() {
            JsonNode node = json.valueToTree(productWithValueObjects());

            assertThat(node.get("price").isDouble()).as("price is a JSON double").isTrue();
            assertThat(node.get("price").doubleValue()).isEqualTo(1599.0);
            assertThat(node.get("price").asText()).isEqualTo("1599.0");

            // an integral price keeps the trailing .0 a double has, rather than gaining
            // the trailing zeros a scale-2 BigDecimal would print
            assertThat(node.get("originalPrice").asText()).isEqualTo("120.5");
        }

        @Test
        void moneyOnAnOrderAndItsLinesIsAlsoADouble() {
            Order order = new Order();
            order.setTotalAmount(Money.of(3198.0));
            order.setItems(List.of(new OrderItem("p1", "GPU", Money.of(109.99), "u", 2)));

            JsonNode node = json.valueToTree(orderMapper.toResponseDto(order));

            assertThat(node.get("totalAmount").isDouble()).isTrue();
            assertThat(node.get("totalAmount").asText()).isEqualTo("3198.0");
            assertThat(node.get("items").get(0).get("price").asText()).isEqualTo("109.99");
        }

        /**
         * The storefront keys its condition filter and its colour map on these exact
         * tokens, and one of them contains a space — so the enum must not fall back to
         * {@code name()}.
         */
        @Test
        void productConditionSerializesAsItsSpacedWireValue() {
            Product product = productWithValueObjects();
            product.setCondition(ProductCondition.LIKE_NEW);

            JsonNode node = json.valueToTree(product);

            assertThat(node.get("condition").isTextual()).isTrue();
            assertThat(node.get("condition").asText()).isEqualTo("LIKE NEW");
        }

        /**
         * The one <em>deliberate</em> wire change in S9, recorded here rather than left to
         * be discovered. A raw {@code ObjectId} serialized as
         * {@code {"timestamp":…,"date":…}} — an unusable shape that no frontend reads
         * (verified by grep across both apps: they consume {@code categoryNames} and send
         * category ids back as the {@code genres} query parameter). {@code CategoryId}
         * publishes the hex string those {@code genres} values already are.
         */
        @Test
        void categoryIdSerializesAsAHexStringRatherThanAnObjectIdStruct() {
            JsonNode node = json.valueToTree(productWithValueObjects());
            JsonNode categoryIds = node.get("categoryIds");

            assertThat(categoryIds.get(0).isTextual()).isTrue();
            assertThat(categoryIds.get(0).asText()).isEqualTo("64b7f0000000000000000001");
            assertThat(categoryIds.get(0).isObject()).as("no longer an ObjectId struct").isFalse();
        }

        @Test
        void roleSerializesAsItsConstantName() {
            User user = new User();
            user.setRole(Role.ADMIN);

            assertThat(json.valueToTree(user).get("role").asText()).isEqualTo("ADMIN");
        }

        /**
         * {@code User.getName()} is a domain seam, not a field. If its {@code @JsonIgnore}
         * were lost, every serialized user would grow a {@code name} object.
         */
        @Test
        void personNameStaysThreeFlatFieldsAndAddsNoNameProperty() {
            User user = new User();
            user.setName(PersonName.of("Jane", "Doe"));

            JsonNode node = json.valueToTree(user);

            assertThat(node.get("firstName").asText()).isEqualTo("Jane");
            assertThat(node.get("lastName").asText()).isEqualTo("Doe");
            assertThat(node.get("fullName").asText()).isEqualTo("Jane Doe");
            assertThat(node.has("name")).as("the value object must not reach the wire").isFalse();
        }

        @Test
        void requestBodiesStillBindFromTheSameJsonTheyAlwaysDid() throws Exception {
            String body = """
                    {"productId":"p1","title":"GPU","author":"NVIDIA","price":24.99,
                     "quantity":2,"image":"http://img/a.png","condition":"LIKE NEW","stock":5}
                    """;

            CartItem item = json.readValue(body, CartItem.class);

            assertThat(item.getPrice()).isEqualTo(Money.of(24.99));
            assertThat(item.getCondition()).isEqualTo(ProductCondition.LIKE_NEW);
            assertThat(item.getQuantity()).isEqualTo(2);
        }

        private Product productWithValueObjects() {
            Product product = new Product();
            product.setId("p1");
            product.setTitle("RTX 4090");
            product.setPrice(Money.of(1599.0));
            product.setOriginalPrice(Money.of(120.5));
            product.setCondition(ProductCondition.NEW);
            product.setStock(5);
            product.setCategoryIds(List.of(CategoryId.of("64b7f0000000000000000001")));
            return product;
        }
    }
}
