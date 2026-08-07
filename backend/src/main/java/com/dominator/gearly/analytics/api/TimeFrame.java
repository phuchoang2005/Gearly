package com.dominator.gearly.analytics.api;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * The reporting window a dashboard query is asked for, bound straight from a {@code ?period=}
 * query parameter.
 *
 * <p>Was {@code model.TimeFrame} — and until S13 it was the only class in the whole
 * {@code model/} package with any behaviour at all ({@link #getStartInstant()}), which the
 * refactoring plan cites as the measure of how anemic that package was. It belongs to the read
 * side: nothing writes it, nothing stores it, and it exists to shape a query.
 */
public enum TimeFrame {
    ALL,
    ONE_MONTH,
    THREE_MONTHS,
    SIX_MONTHS,
    ONE_YEAR;

    public Instant getStartInstant() {
        // Instant doesn't support MONTHS/YEARS units; compute calendar offsets on a
        // date-time (UTC) then convert back.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return switch(this) {
            case ALL -> null;
            case ONE_MONTH -> now.minusMonths(1).toInstant();
            case THREE_MONTHS -> now.minusMonths(3).toInstant();
            case SIX_MONTHS -> now.minusMonths(6).toInstant();
            case ONE_YEAR -> now.minusYears(1).toInstant();
        };
    }
}
