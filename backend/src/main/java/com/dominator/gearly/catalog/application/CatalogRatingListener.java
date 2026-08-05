package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.reviews.domain.ReviewApproved;
import com.dominator.gearly.reviews.domain.ReviewRejected;
import com.dominator.gearly.shared.domain.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * What the catalog does when a review is moderated: a published review counts towards its
 * product's rating, and a withdrawn one stops counting.
 *
 * <h2>Why this listener is the fix, not just a relocation</h2>
 * The rollup used to happen at submission. {@code ReviewService.createReview} called
 * {@code product.addRating(...)} in the same loop that built the reviews, while every one of
 * them was still {@code PENDING}. So {@code averageRating} counted reviews a moderator later
 * rejected and nobody ever saw — while the star histogram on the same page queried
 * {@code {status: 'APPROVED'}}. The plan calls the two numbers structurally inconsistent, and
 * they were: not drifting apart through a bug, but computed from different sets of reviews by
 * design. Driving the rollup from moderation is what makes them the same set.
 *
 * <p>The sibling of {@code CatalogStockListener}, and the same shape: the catalog reacts for
 * itself to an event another context publishes, rather than the reviews context holding a
 * {@code ProductRepository} and writing to somebody else's aggregate.
 *
 * <h2>Why {@code BEFORE_COMMIT}</h2>
 * Because a review's status and the rating it contributes are one fact. {@code AFTER_COMMIT}
 * would allow a published review whose stars were never counted — the mirror of the oversell
 * {@code CatalogStockListener} guards against, and just as impossible to notice: nothing would
 * error, the average would simply be quietly wrong and would stay wrong until somebody
 * recomputed it. Running inside the caller's transaction means the moderation and the rollup
 * commit together or not at all.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogRatingListener {

    private final ProductRepository products;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(ReviewApproved event) {
        apply(event.productId(), product -> product.addRating(event.rating()),
                "approved review " + event.reviewId());
    }

    /**
     * A rejection only takes stars away if it had them: a {@code PENDING → REJECTED} review was
     * never counted, and subtracting anyway would drag the average down by a review nobody ever
     * saw. {@link ReviewRejected#withdrawsACountedRating()} carries that decision, made from the
     * status the review is leaving.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(ReviewRejected event) {
        if (!event.withdrawsACountedRating()) {
            return;
        }
        apply(event.productId(), product -> product.removeRating(event.rating()),
                "withdrawn review " + event.reviewId());
    }

    /**
     * A product deleted since the review was written is skipped with a warning rather than
     * failing the moderation — the same call {@code CatalogStockListener} makes for a cancelled
     * order, and for the same reason: there is no rollup to adjust, and refusing to let a
     * moderator act on a review because its product is gone helps nobody.
     */
    private void apply(ProductId productId, java.util.function.Consumer<Product> change, String what) {
        products.findById(productId).ifPresentOrElse(
                product -> {
                    change.accept(product);
                    products.save(product);
                },
                () -> log.warn("Cannot apply {} to product {}: it no longer exists", what, productId));
    }
}
