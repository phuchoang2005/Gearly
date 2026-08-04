/**
 * The translation layer that keeps {@code shared.domain}'s value objects invisible
 * outside the JVM: a {@code MongoCustomConversions} bean of read/write converter pairs,
 * and a Jackson module with the matching {@code @JsonValue}/{@code @JsonCreator}
 * behavior.
 *
 * <p><b>Layer contract, and the load-bearing constraint of the whole refactor:</b> each
 * converter must write back exactly the BSON type the current documents already use —
 * {@code Money} → {@code double}, {@code Quantity} → {@code int}, {@code ProductId} →
 * {@code String}, {@code CategoryId} → {@code ObjectId}. Introducing the value objects is
 * then a pure compile-time change: no document migration, no wire-format change, both
 * frontends untouched. {@code ResponseDtoWireCompatTest} and a stored-BSON-type round-trip
 * test are what hold that line.
 *
 * @see com.dominator.gearly.shared
 */
package com.dominator.gearly.shared.infrastructure;
