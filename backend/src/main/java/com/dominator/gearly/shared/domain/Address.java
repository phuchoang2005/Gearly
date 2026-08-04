package com.dominator.gearly.shared.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A postal address, embedded in whatever holds it — a {@code User}'s profile in Identity,
 * a {@code ShippingInformation} on an order in Ordering.
 *
 * <p>It lives in the shared kernel because two contexts genuinely share the concept, and
 * the alternative — a copy each — would mean the geo lookup that fills in
 * {@code cityId}/{@code stateId}/{@code countryId} had to know about both.
 *
 * <h2>Why the {@code @Document} came off</h2>
 * It declared {@code collection = "address"}, a collection that does not exist. Addresses
 * are only ever embedded, never stored standalone, so the annotation was the same
 * copy-paste artifact carried by {@code OrderItem}, {@code Payment} and
 * {@code Transaction}. Spring Data reads {@code @Document} only when mapping a top-level
 * entity, so removing it changes nothing about how an address is stored.
 *
 * <h2>Why it is still a mutable Lombok bag</h2>
 * Deliberate, and temporary. S10 needed this type out of {@code model/} so that
 * {@code ShippingInformation} could move into {@code ordering.domain} without dragging a
 * legacy import behind it — {@code domain_does_not_reach_back_into_legacy_packages} would
 * have failed. Turning it into a proper immutable value object touches the whole Identity
 * registration and profile-update path, which is S12's work.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String street;
    private String city;
    private int cityId;
    private String state;
    private int stateId;
    private String postalCode;
    private String country;
    private int countryId;
}
