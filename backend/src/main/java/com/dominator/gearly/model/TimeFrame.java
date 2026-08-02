package com.dominator.gearly.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
