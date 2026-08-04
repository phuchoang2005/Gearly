package com.dominator.gearly.shared.infrastructure;

import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.convert.MongoConversionContext;
import org.springframework.data.mongodb.core.convert.MongoValueConverter;

/**
 * Property-level converters for the handful of fields that store an otherwise
 * string-backed id as a BSON {@code ObjectId}.
 *
 * <h2>Why these cannot be global converters</h2>
 * {@code ProductId} is stored as a {@code String} in {@code orders.items[].productId} and
 * {@code carts.items[].productId}, but as an {@code ObjectId} in {@code reviews.productId}
 * — the same Java type with two different BSON representations depending on which document
 * it sits in. A {@code MongoCustomConversions} pair is registered per type and so cannot
 * express that; {@link org.springframework.data.convert.ValueConverter @ValueConverter}
 * is applied per property and can.
 *
 * <p>The global {@code ProductId ↔ String} pair in {@link DomainTypeConverters} therefore
 * stays the default, and the three {@code Review} fields opt out of it individually. Both
 * representations are left exactly as they are on disk — which is the whole point, since
 * the reviews collection is joined against products by {@code ObjectId} in the rating
 * distribution aggregation.
 *
 * <p>The inconsistency itself is not fixed here, only made type-safe and explicit. S12
 * owns the reviews context and can decide whether to normalize the stored form.
 */
public final class ObjectIdBackedIdConverters {

    private ObjectIdBackedIdConverters() {
    }

    public static class ProductIdAsObjectId implements MongoValueConverter<ProductId, ObjectId> {

        @Override
        public ProductId read(ObjectId value, MongoConversionContext context) {
            return value == null ? null : ProductId.of(value.toHexString());
        }

        @Override
        public ObjectId write(ProductId value, MongoConversionContext context) {
            return value == null ? null : new ObjectId(value.value());
        }
    }

    public static class OrderIdAsObjectId implements MongoValueConverter<OrderId, ObjectId> {

        @Override
        public OrderId read(ObjectId value, MongoConversionContext context) {
            return value == null ? null : OrderId.of(value.toHexString());
        }

        @Override
        public ObjectId write(OrderId value, MongoConversionContext context) {
            return value == null ? null : new ObjectId(value.value());
        }
    }

    public static class UserIdAsObjectId implements MongoValueConverter<UserId, ObjectId> {

        @Override
        public UserId read(ObjectId value, MongoConversionContext context) {
            return value == null ? null : UserId.of(value.toHexString());
        }

        @Override
        public ObjectId write(UserId value, MongoConversionContext context) {
            return value == null ? null : new ObjectId(value.value());
        }
    }
}
