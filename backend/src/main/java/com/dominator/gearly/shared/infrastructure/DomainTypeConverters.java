package com.dominator.gearly.shared.infrastructure;

import com.dominator.gearly.shared.domain.CartId;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.ReviewId;
import com.dominator.gearly.shared.domain.Role;
import com.dominator.gearly.shared.domain.Slug;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

/**
 * Read/write converter pairs that keep {@code shared.domain}'s value objects invisible
 * outside the JVM.
 *
 * <h2>This is the load-bearing class of Sprint 9</h2>
 * Every converter here writes back <em>exactly</em> the BSON type the existing documents
 * already hold: {@code Money} → {@code double}, {@code Quantity} and {@code Rating} →
 * {@code int}, the string-backed ids and value objects → {@code String}, and
 * {@link CategoryId} → {@code ObjectId}. That is what makes introducing the value objects
 * a pure compile-time change — no document migration, no wire-format change, neither
 * frontend touched. If one of these pairs stops being symmetric, the premise of the whole
 * refactor collapses; {@code DomainTypeBsonRoundTripTest} is what holds the line, by
 * asserting the stored BSON types directly rather than trusting the round trip.
 *
 * <p><b>S12:</b> {@link CategoryId} is now genuinely the only one. S9 shipped a companion
 * {@code ObjectIdBackedIdConverters} because {@code reviews} stored its three ids as
 * {@code ObjectId}s — one Java type with two BSON forms, which a per-<em>type</em> registration
 * like this one cannot express. S12 normalized those three to strings, so the per-property
 * converters are gone and every pair a document needs is registered here.
 *
 * <h2>Two things worth knowing</h2>
 * <b>Registering a writing converter is also what makes a type "simple".</b> Without one,
 * Spring Data would treat {@code Money} as an entity and store it as a nested document
 * ({@code {amount: …, currency: …}}) instead of a bare number.
 *
 * <p><b>Numbers are read widely and written narrowly.</b> The seed data holds integral
 * prices as BSON {@code int32} ({@code "originalPrice": 195}) alongside {@code double}s,
 * so the reading side accepts any {@link Number}. The writing side always emits a
 * {@code double} — which is what the current {@code double}-typed fields already do on
 * every save, so this introduces no new type churn.
 */
@Configuration
public class DomainTypeConverters {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                // Money
                new MoneyToDoubleConverter(),
                new NumberToMoneyConverter(),

                // Counts and ratings
                new QuantityToIntegerConverter(),
                new NumberToQuantityConverter(),
                new RatingToIntegerConverter(),
                new NumberToRatingConverter(),

                // Typed ids stored as plain strings
                new ProductIdToStringConverter(),
                new StringToProductIdConverter(),
                new OrderIdToStringConverter(),
                new StringToOrderIdConverter(),
                new UserIdToStringConverter(),
                new StringToUserIdConverter(),
                new CartIdToStringConverter(),
                new StringToCartIdConverter(),
                new ReviewIdToStringConverter(),
                new StringToReviewIdConverter(),

                // The one id stored as an ObjectId
                new CategoryIdToObjectIdConverter(),
                new ObjectIdToCategoryIdConverter(),

                // String-backed value objects
                new EmailAddressToStringConverter(),
                new StringToEmailAddressConverter(),
                new PhoneNumberToStringConverter(),
                new StringToPhoneNumberConverter(),
                new SlugToStringConverter(),
                new StringToSlugConverter(),

                // Enums whose stored token is not the constant name
                new ProductConditionToStringConverter(),
                new StringToProductConditionConverter(),
                new RoleToStringConverter(),
                new StringToRoleConverter()));
    }

    // ------------------------------------------------------------------------
    // Money
    // ------------------------------------------------------------------------

    @WritingConverter
    static class MoneyToDoubleConverter implements Converter<Money, Double> {
        @Override
        public Double convert(Money source) {
            return source.toDouble();
        }
    }

    /** Accepts {@code int32}, {@code int64} and {@code double} — all three are in the data. */
    @ReadingConverter
    static class NumberToMoneyConverter implements Converter<Number, Money> {
        @Override
        public Money convert(Number source) {
            return Money.of(source.doubleValue());
        }
    }

    // ------------------------------------------------------------------------
    // Counts and ratings
    // ------------------------------------------------------------------------

    @WritingConverter
    static class QuantityToIntegerConverter implements Converter<Quantity, Integer> {
        @Override
        public Integer convert(Quantity source) {
            return source.toInt();
        }
    }

    @ReadingConverter
    static class NumberToQuantityConverter implements Converter<Number, Quantity> {
        @Override
        public Quantity convert(Number source) {
            return Quantity.of(source.intValue());
        }
    }

    @WritingConverter
    static class RatingToIntegerConverter implements Converter<Rating, Integer> {
        @Override
        public Integer convert(Rating source) {
            return source.toInt();
        }
    }

    @ReadingConverter
    static class NumberToRatingConverter implements Converter<Number, Rating> {
        @Override
        public Rating convert(Number source) {
            return Rating.of(source.intValue());
        }
    }

    // ------------------------------------------------------------------------
    // Typed ids stored as plain strings
    // ------------------------------------------------------------------------

    @WritingConverter
    static class ProductIdToStringConverter implements Converter<ProductId, String> {
        @Override
        public String convert(ProductId source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToProductIdConverter implements Converter<String, ProductId> {
        @Override
        public ProductId convert(String source) {
            return ProductId.of(source);
        }
    }

    @WritingConverter
    static class OrderIdToStringConverter implements Converter<OrderId, String> {
        @Override
        public String convert(OrderId source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToOrderIdConverter implements Converter<String, OrderId> {
        @Override
        public OrderId convert(String source) {
            return OrderId.of(source);
        }
    }

    @WritingConverter
    static class UserIdToStringConverter implements Converter<UserId, String> {
        @Override
        public String convert(UserId source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToUserIdConverter implements Converter<String, UserId> {
        @Override
        public UserId convert(String source) {
            return UserId.of(source);
        }
    }

    @WritingConverter
    static class CartIdToStringConverter implements Converter<CartId, String> {
        @Override
        public String convert(CartId source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToCartIdConverter implements Converter<String, CartId> {
        @Override
        public CartId convert(String source) {
            return CartId.of(source);
        }
    }

    @WritingConverter
    static class ReviewIdToStringConverter implements Converter<ReviewId, String> {
        @Override
        public String convert(ReviewId source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToReviewIdConverter implements Converter<String, ReviewId> {
        @Override
        public ReviewId convert(String source) {
            return ReviewId.of(source);
        }
    }

    // ------------------------------------------------------------------------
    // CategoryId — the one id stored as an ObjectId
    // ------------------------------------------------------------------------

    @WritingConverter
    static class CategoryIdToObjectIdConverter implements Converter<CategoryId, ObjectId> {
        @Override
        public ObjectId convert(CategoryId source) {
            return new ObjectId(source.value());
        }
    }

    @ReadingConverter
    static class ObjectIdToCategoryIdConverter implements Converter<ObjectId, CategoryId> {
        @Override
        public CategoryId convert(ObjectId source) {
            return CategoryId.of(source.toHexString());
        }
    }

    // ------------------------------------------------------------------------
    // String-backed value objects
    // ------------------------------------------------------------------------

    @WritingConverter
    static class EmailAddressToStringConverter implements Converter<EmailAddress, String> {
        @Override
        public String convert(EmailAddress source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToEmailAddressConverter implements Converter<String, EmailAddress> {
        @Override
        public EmailAddress convert(String source) {
            return EmailAddress.of(source);
        }
    }

    @WritingConverter
    static class PhoneNumberToStringConverter implements Converter<PhoneNumber, String> {
        @Override
        public String convert(PhoneNumber source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToPhoneNumberConverter implements Converter<String, PhoneNumber> {
        @Override
        public PhoneNumber convert(String source) {
            return PhoneNumber.of(source);
        }
    }

    @WritingConverter
    static class SlugToStringConverter implements Converter<Slug, String> {
        @Override
        public String convert(Slug source) {
            return source.value();
        }
    }

    @ReadingConverter
    static class StringToSlugConverter implements Converter<String, Slug> {
        @Override
        public Slug convert(String source) {
            return Slug.of(source);
        }
    }

    // ------------------------------------------------------------------------
    // Enums whose stored token is not the constant name
    // ------------------------------------------------------------------------

    @WritingConverter
    static class ProductConditionToStringConverter implements Converter<ProductCondition, String> {
        @Override
        public String convert(ProductCondition source) {
            return source.wireValue();
        }
    }

    @ReadingConverter
    static class StringToProductConditionConverter implements Converter<String, ProductCondition> {
        @Override
        public ProductCondition convert(String source) {
            return ProductCondition.fromWireValue(source);
        }
    }

    /**
     * {@link Role}'s constant names already match the stored tokens, so Spring Data's
     * built-in enum handling would do the right thing. Registered explicitly anyway, so
     * that the stored representation of a domain enum is stated in one place rather than
     * inherited from a framework default that a later refactor could quietly change.
     */
    @WritingConverter
    static class RoleToStringConverter implements Converter<Role, String> {
        @Override
        public String convert(Role source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToRoleConverter implements Converter<String, Role> {
        @Override
        public Role convert(String source) {
            return Role.fromValue(source);
        }
    }
}
