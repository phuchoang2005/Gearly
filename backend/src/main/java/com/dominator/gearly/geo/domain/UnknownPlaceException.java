package com.dominator.gearly.geo.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * A place name that is not in the reference dataset.
 *
 * <p><b>This replaces a 500.</b> {@code AddressService} answered every failed lookup with
 * {@code .orElse(null)}, and {@code AuthService.resolveAddress} assigned the result to an
 * {@code int}:
 *
 * <pre>int countryId = addressService.getCountryIdByName(command.country());</pre>
 *
 * <p>Unboxing a null throws {@link NullPointerException}, so registering with a country the
 * dataset did not have — or, because the field is not {@code @NotBlank}, with no country at all
 * — crashed the request. The caller was told the server was broken when what had actually
 * happened was that they submitted something the server did not recognise.
 *
 * <p>A blank name is now {@link ResolvedPlace#NONE} rather than an error, since the registration
 * DTO genuinely treats the address as optional; a name that was given and does not match is a
 * 400 that says which one.
 */
public class UnknownPlaceException extends DomainRuleViolationException {

    public UnknownPlaceException(String kind, String name) {
        super("Unknown " + kind + ": '" + name + "'");
    }
}
