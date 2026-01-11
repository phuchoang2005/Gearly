package com.dominator.bookify.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public enum TimeFrame {
    ALL,
    ONE_MONTH,
    THREE_MONTHS,
    SIX_MONTHS,
    ONE_YEAR;

    public Instant getStartInstant() {
        Instant now = Instant.now();
        return switch(this) {
            case ALL -> null;
            case ONE_MONTH -> now.minus(1, ChronoUnit.MONTHS);
            case THREE_MONTHS -> now.minus(3, ChronoUnit.MONTHS);
            case SIX_MONTHS -> now.minus(6, ChronoUnit.MONTHS);
            case ONE_YEAR -> now.minus(1, ChronoUnit.YEARS);
        };
    }
}
