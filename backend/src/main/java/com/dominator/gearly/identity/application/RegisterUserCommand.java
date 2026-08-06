package com.dominator.gearly.identity.application;

/**
 * What a registration needs, as the application layer's own vocabulary rather than the
 * request body's.
 *
 * <p>Note what is absent: {@code fullName}. The registration DTO still accepts one because both
 * frontends still send it, but it has been ignored since S9 — honouring a client-supplied
 * display name is what let an account be created whose {@code fullName} disagreed with its own
 * first and last name, permanently and with nothing to reconcile the two. Leaving it out of the
 * command is what makes that structural rather than a line of code somebody could put back.
 */
public record RegisterUserCommand(String firstName,
                                  String lastName,
                                  String email,
                                  String password,
                                  String phone,
                                  String streetAddress,
                                  String city,
                                  String state,
                                  String postalCode,
                                  String country) {
}
