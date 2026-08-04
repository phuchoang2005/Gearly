/**
 * The value objects, typed identifiers and domain primitives every context is written in:
 * {@code Money}, {@code Quantity}, {@code Rating}, {@code EmailAddress},
 * {@code PhoneNumber}, {@code PersonName}, {@code Slug}, the {@code *Id} record types, and
 * the {@code AggregateRoot} / {@code DomainEvent} base types.
 *
 * <p><b>Layer contract</b> (enforced by {@code ArchitectureFitnessTest}): zero framework
 * dependencies beyond Spring Data's annotation types — no web, no security, no repository
 * types. Every type here is immutable, self-validating in its constructor, and
 * constructible in a unit test with a plain {@code new}.
 *
 * <p><b>Because it is shared, it is rigid.</b> A change here reaches every context at
 * once. Prefer adding a type over widening an existing one.
 *
 * @see com.dominator.gearly.shared
 */
package com.dominator.gearly.shared.domain;
