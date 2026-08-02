package com.dominator.gearly.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TimeFrame#getStartInstant()} feeds the sales-analytics date filter.
 * {@code Instant} does not support MONTHS/YEARS units, so the calendar frames
 * must be computed via a date-time type — these assertions guard that.
 */
class TimeFrameTest {

    @Test
    void all_hasNoLowerBound() {
        assertThat(TimeFrame.ALL.getStartInstant()).isNull();
    }

    @Test
    void oneMonth_isRoughlyOneMonthAgo() {
        Instant now = Instant.now();
        Instant start = TimeFrame.ONE_MONTH.getStartInstant();

        assertThat(start).isNotNull();
        assertThat(start).isBefore(now);
        // between ~28 and ~31 days ago — comfortably inside a [26, 40] day window
        assertThat(start).isAfter(now.minus(40, ChronoUnit.DAYS));
        assertThat(start).isBefore(now.minus(26, ChronoUnit.DAYS));
    }

    @Test
    void oneYear_isRoughlyOneYearAgo() {
        Instant now = Instant.now();
        Instant start = TimeFrame.ONE_YEAR.getStartInstant();

        assertThat(start).isNotNull();
        assertThat(start).isAfter(now.minus(370, ChronoUnit.DAYS));
        assertThat(start).isBefore(now.minus(360, ChronoUnit.DAYS));
    }
}
