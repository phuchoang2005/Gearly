package com.dominator.gearly.shared.domain;

/**
 * A product's rating rollup — the review count and the running star total — with the
 * average derived rather than stored alongside them.
 *
 * <p>{@code Product} currently keeps {@code ratingCount}, {@code totalRating} and
 * {@code averageRating} as three independent, individually settable fields, so nothing
 * stops them from disagreeing; {@code ReviewService.applyRating} is the only code that
 * happens to keep them consistent. Here they are one value with one invariant: the
 * average is a function of the other two and cannot be set.
 *
 * <p><b>Not yet a persisted field.</b> Folding the three columns into one nested document
 * would change the stored shape, which S9 forbids. {@code Product} keeps its three fields
 * and S11 adopts this type when {@code Product} becomes an aggregate with
 * {@code addRating(Rating)}.
 */
public record ProductRating(int count, int total) {

    public static final ProductRating NONE = new ProductRating(0, 0);

    public ProductRating {
        if (count < 0) {
            throw new IllegalArgumentException("rating count must not be negative, was " + count);
        }
        if (total < 0) {
            throw new IllegalArgumentException("rating total must not be negative, was " + total);
        }
        if (count == 0 && total != 0) {
            throw new IllegalArgumentException("a rating total of " + total + " needs at least one rating");
        }
        if (count > 0 && (total < count * Rating.MIN || total > count * Rating.MAX)) {
            throw new IllegalArgumentException(
                    "a total of " + total + " is impossible across " + count + " ratings");
        }
    }

    public ProductRating add(Rating rating) {
        return new ProductRating(count + 1, total + rating.value());
    }

    public ProductRating remove(Rating rating) {
        return new ProductRating(count - 1, total - rating.value());
    }

    /**
     * The average, rounded to two decimals.
     *
     * <p>Reproduces the existing {@code Math.round(avg * 100) / 100.0} arithmetic from
     * {@code ReviewService.applyRating} exactly, so adopting this type cannot move a
     * single stored average.
     */
    public double average() {
        if (count == 0) {
            return 0.0;
        }
        return Math.round((double) total / count * 100) / 100.0;
    }

    public boolean isUnrated() {
        return count == 0;
    }
}
