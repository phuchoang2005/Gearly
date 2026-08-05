package com.dominator.gearly.shared.domain;

import java.time.Instant;

/**
 * Something that happened in the domain, stated in the past tense and as a value.
 *
 * <p>Events are how one aggregate reaches another without holding a reference to it. The
 * working agreement is "one aggregate per transaction"; anything that has to touch a second
 * one — placing an order also decrements catalog stock and empties a cart — goes out as an
 * event rather than as a direct call from inside the first aggregate's use case.
 *
 * <p>A published event is part of a context's contract, so it carries values and ids and never
 * an aggregate. A listener that received an {@code Order} could change it, which would put a
 * second aggregate's write back inside the first one's transaction — exactly what the events
 * exist to avoid.
 */
public interface DomainEvent {

    /** When the thing happened, not when the listener got round to it. */
    Instant occurredOn();
}
