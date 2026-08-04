/**
 * <b>Shared kernel.</b> The vocabulary every context is written in: value objects
 * ({@code Money}, {@code Quantity}, {@code Rating}, {@code EmailAddress},
 * {@code PhoneNumber}, {@code PersonName}, {@code Slug}), typed identifiers, the
 * {@code AggregateRoot} base and the {@code DomainEvent} marker — plus the Mongo and
 * Jackson converters that keep those types invisible on the wire and in the database.
 *
 * <p><b>Relationships:</b> every context may depend on {@code shared.domain}. Nothing in
 * {@code shared} may depend on a context. Because it is shared, a change here is a change
 * to every context at once — extend it deliberately.
 *
 * <p><b>Load-bearing constraint:</b> {@code shared.infrastructure}'s
 * {@code MongoCustomConversions} must write each value object back to the primitive BSON
 * type the current documents use ({@code Money} → {@code double}, {@code ProductId} →
 * {@code String}, …). If the stored shape moves, the "no migration required" premise of
 * the whole refactor collapses.
 *
 * <p>Filled in by <b>Sprint 9</b>.
 */
package com.dominator.gearly.shared;
