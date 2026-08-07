package com.dominator.gearly.geo.domain;

/**
 * The three numeric ids an {@code Address} stores, resolved from the names a registration form
 * submits.
 *
 * <h2>Why zero means "not given"</h2>
 * {@code shared.domain.Address} declares {@code cityId}, {@code stateId} and {@code countryId}
 * as primitive {@code int}s, so there is no null to store and zero is the only value available
 * for "the customer did not say". That is a property of the address type rather than a choice
 * made here, and it is the shape the stored documents already have.
 *
 * <p>It is <em>only</em> used for absent input. A name that was supplied but does not match the
 * dataset raises {@link UnknownPlaceException} rather than quietly resolving to zero — the two
 * are different situations and collapsing them is how a typo becomes a silently corrupt address.
 */
public record ResolvedPlace(int countryId, int stateId, int cityId) {

    /** Nothing was supplied, or nothing that could be looked up. */
    public static final ResolvedPlace NONE = new ResolvedPlace(0, 0, 0);
}
