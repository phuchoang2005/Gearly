package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A star rating, 1 to 5 inclusive.
 *
 * <p>Today {@code CreateReviewRequestDTO.rating} is an unbounded {@code int} and
 * {@code ReviewService.applyRating} folds whatever arrives into the product's running
 * total — a rating of {@code 900} is accepted and permanently skews {@code averageRating}
 * (pinned as a {@code KNOWN BUG} in the S8 characterization suite). This type is the fix,
 * but note <b>the field swap is not S9's</b>: {@code Review.rating} stays an {@code int}
 * until S12 puts the review lifecycle behind an aggregate, because a legacy document with
 * an out-of-range rating must not become unreadable in the meantime.
 *
 * <p>Serializes as a bare {@code int} on both the wire and in Mongo.
 */
public record Rating(int value) implements Comparable<Rating> {

    public static final int MIN = 1;
    public static final int MAX = 5;

    public Rating {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    "rating must be between " + MIN + " and " + MAX + ", was " + value);
        }
    }

    @JsonCreator
    public static Rating of(int value) {
        return new Rating(value);
    }

    @JsonValue
    public int toInt() {
        return value;
    }

    @Override
    public int compareTo(Rating other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
