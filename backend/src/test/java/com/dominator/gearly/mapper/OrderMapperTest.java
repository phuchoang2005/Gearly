package com.dominator.gearly.mapper;

import com.dominator.gearly.model.Image;
import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toOrderItem_snapshotsPriceTitleImageAndQuantity() {
        Product product = new Product();
        product.setId("p1");
        product.setTitle("RTX 4090");
        product.setPrice(1599.0);
        product.setImages(List.of(new Image("http://img/first.png", "gpu"),
                new Image("http://img/second.png", "gpu-back")));

        OrderItem item = mapper.toOrderItem(product, 3);

        assertThat(item.getProductId()).isEqualTo("p1");
        assertThat(item.getTitle()).isEqualTo("RTX 4090");
        assertThat(item.getPrice()).isEqualTo(1599.0);
        assertThat(item.getQuantity()).isEqualTo(3);
        // snapshot uses the product's first image
        assertThat(item.getImageUrl()).isEqualTo("http://img/first.png");
    }
}
