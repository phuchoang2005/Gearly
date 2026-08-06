package com.dominator.gearly.platform.security;

import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.shared.domain.UserId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The signed-in caller, as Spring Security understands one.
 *
 * <p>This type is where the framework's idea of "who is calling" stops. A controller unwraps it
 * into a {@link UserId} and calls in with that; no application service, domain class or
 * repository adapter may name it, which is now enforced twice over —
 * {@code security_types_stop_at_the_api_layer} bans Spring Security types from a context, and
 * {@code contexts_do_not_depend_on_the_platform} bans this package outright.
 *
 * <p>It moved here from {@code com.dominator.gearly.security} in S12 as part of that: the whole
 * access boundary is platform's, and a context that cannot see the package cannot accidentally
 * take a {@code UserDetails} as a parameter again.
 */
@RequiredArgsConstructor
@Getter
public class AuthenticatedUser implements UserDetails {

    private final User user;

    /** The typed id a use case takes. The only thing most controllers need from the principal. */
    public UserId id() {
        return user.userId();
    }

    /**
     * {@code ROLE_ADMIN} / {@code ROLE_CUSTOMER}, unchanged — the URL rules and the
     * {@code @PreAuthorize} expressions both read these.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail() == null ? null : user.getEmail().value();
    }
}
