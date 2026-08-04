/**
 * <b>Analytics — the query side.</b> The admin dashboard's read models: sales roll-ups,
 * top sellers, quantity sold, low-stock lists. This is the explicit CQRS split — analytics
 * reads documents and returns DTOs, and never loads, mutates or even names a domain
 * aggregate.
 *
 * <p><b>The one privilege of this package:</b> it is the only place in the codebase
 * allowed to use {@code MongoTemplate}. Aggregation pipelines belong here; everywhere else
 * goes through a repository port. {@code ArchitectureFitnessTest} enforces this.
 *
 * <p><b>The price of that privilege:</b> analytics reads the raw document shape, so it is
 * coupled to field names rather than to types. That coupling is guarded by integration
 * tests against a real MongoDB, not by the compiler.
 *
 * <p><b>Relationships:</b> downstream of Ordering and Catalog, one-way, read-only.
 *
 * <p>Filled in by <b>Sprint 13</b>.
 */
package com.dominator.gearly.analytics;
