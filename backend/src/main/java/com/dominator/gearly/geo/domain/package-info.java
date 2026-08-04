/**
 * The domain model for this context: aggregates, entities, value objects, domain
 * services, domain events and the repository/external-system <em>ports</em> they are
 * expressed in terms of.
 *
 * <p><b>Layer contract</b> (enforced by {@code ArchitectureFitnessTest}): this package
 * may depend only on the JDK, Lombok's {@code @Getter}, Spring Data's mapping and
 * annotation types, and {@code shared.domain}. Web, security, HTTP and Spring Data
 * repository types are banned here — the domain must be testable with a plain
 * constructor call and no Spring context.
 *
 * @see com.dominator.gearly.geo
 */
package com.dominator.gearly.geo.domain;
