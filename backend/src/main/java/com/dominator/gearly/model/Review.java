package com.dominator.gearly.model;

import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;
import com.dominator.gearly.shared.infrastructure.ObjectIdBackedIdConverters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.convert.ValueConverter;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "reviews")
public class Review {
    @Id
    private String id;

    /**
     * These three are the id asymmetry the shared kernel absorbs: they are stored as BSON
     * {@code ObjectId} while the same id types are stored as plain strings everywhere else
     * (an order's {@code items[].productId}, a cart's line, {@code Order.userId}). The
     * {@code @ValueConverter}s keep the stored form exactly as it is — the rating
     * distribution aggregation joins on these as {@code ObjectId} — while the Java side
     * stops being three interchangeable {@code ObjectId}s that nothing prevents swapping.
     */
    @ValueConverter(ObjectIdBackedIdConverters.ProductIdAsObjectId.class)
    private ProductId productId;

    @ValueConverter(ObjectIdBackedIdConverters.OrderIdAsObjectId.class)
    private OrderId orderId;

    @ValueConverter(ObjectIdBackedIdConverters.UserIdAsObjectId.class)
    private UserId userId;

    /**
     * Deliberately still an unbounded {@code int}, not a {@code Rating}. S12 owns the review
     * lifecycle; until then a legacy document carrying the out-of-range rating pinned as a
     * KNOWN BUG by the S8 characterization suite has to stay readable.
     */
    private int rating;

    private String subject;
    private String comment;
    private ReviewStatus status = ReviewStatus.PENDING;
    @CreatedDate
    private String addedAt;
    @LastModifiedDate
    private String modifiedAt;
}
