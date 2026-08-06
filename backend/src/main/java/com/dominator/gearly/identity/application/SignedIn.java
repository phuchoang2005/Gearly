package com.dominator.gearly.identity.application;

import com.dominator.gearly.identity.domain.User;

/**
 * The result of a successful sign-in: a bearer token and the account it identifies.
 *
 * <p>A record rather than {@code LoginResponseDTO}, because the application layer does not own
 * the wire format — {@code identity.api}'s mapper turns this into the JSON both frontends read,
 * and that is the only place the response shape is described. The same split S10 made when
 * {@code OrderQueryService} started returning {@code Order} instead of a DTO.
 */
public record SignedIn(String token, User user) {
}
