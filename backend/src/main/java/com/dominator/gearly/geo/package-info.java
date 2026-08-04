/**
 * <b>Geo — generic subdomain.</b> The country/state/city reference data behind the
 * shipping-address form. Read-mostly lookup tables with no business rules of their own.
 *
 * <p><b>Relationships:</b> Ordering's {@code ShippingInformation} references geo entries
 * by id. Geo depends on no context.
 *
 * <p>Filled in by <b>Sprint 13</b>.
 */
package com.dominator.gearly.geo;
