/**
 * <b>Reviews — supporting.</b> Owns customer product reviews and their moderation
 * lifecycle: submission against a delivered order, approval, rejection, and the queries
 * behind the star histogram.
 *
 * <p><b>Aggregate:</b> {@code Review} (root). Invariants: the rating is a
 * {@code Rating} value object bounded to 1–5; {@code ReviewStatus} transitions are
 * one-way out of {@code PENDING} (an approved review cannot silently return to the
 * queue); a given order may be reviewed once.
 *
 * <p><b>Relationships:</b>
 * <ul>
 *   <li><b>Reviews → Catalog</b> — event-driven. {@code ReviewApproved} and
 *       {@code ReviewRejected} adjust {@code Product}'s rating roll-up. This is what makes
 *       {@code averageRating} and the {@code APPROVED}-filtered histogram agree; today
 *       they are computed from different populations.</li>
 *   <li><b>Ordering → Reviews</b> — a review names its order by {@code OrderId} and is
 *       only accepted for an order in a reviewable status.</li>
 *   <li><b>Identity → Reviews</b> — an author is a {@code UserId}.</li>
 * </ul>
 *
 * <p><b>Published events:</b> {@code ReviewApproved}, {@code ReviewRejected}.
 *
 * <p>Filled in by <b>Sprint 12</b>.
 */
package com.dominator.gearly.reviews;
