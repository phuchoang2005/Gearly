package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * The base class of an aggregate root: the entity that owns a consistency boundary and is the
 * only way in or out of it. Its one job here is collecting the {@link DomainEvent}s its
 * behavior raises, so a use case does not have to remember to publish them by hand.
 *
 * <h2>How the events get out</h2>
 * The aggregate records; the repository adapter publishes. {@code MongoOrderRepository.save}
 * drains {@link #pullDomainEvents()} after the write and hands each event to Spring's
 * {@code ApplicationEventPublisher}. That places publication at exactly one point — a use case
 * cannot forget, and it cannot publish an event for a change that failed to persist.
 *
 * <p>Draining is destructive on purpose. An aggregate saved twice in one request must not
 * announce the same thing twice.
 *
 * <h2>Why the list is invisible to Mongo and to Jackson</h2>
 * An aggregate root in this codebase <em>is</em> the mapped {@code @Document} and is
 * serialized straight onto the wire, so an ordinary field here would become a stored array and
 * a JSON property. {@code @Transient} keeps it out of the document and {@code @JsonIgnore} out
 * of the response; {@code transient} keeps it out of Java serialization too. The wire-shape
 * test would catch a miss, but the annotations state the intent.
 */
public abstract class AggregateRoot {

    @Transient
    @JsonIgnore
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /** Hands over everything recorded so far and forgets it. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pending = List.copyOf(domainEvents);
        domainEvents.clear();
        return pending;
    }
}
