package com.dominator.gearly.platform.security;

import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Turns a {@code Bearer} token into an authenticated principal, or leaves the request
 * anonymous. Never into an error.
 *
 * <h2>The 500-instead-of-401 bug, and why the shape of this class is the fix</h2>
 * This filter used to read {@code jwtUtil.extractEmail(token)} on the line <em>before</em>
 * {@code jwtUtil.validateToken(token)}. Extraction parses, and parsing an expired or tampered
 * token throws — so validation never ran, and a {@code JwtException} propagated out of the
 * filter chain. That is not something Spring Security renders as a challenge; it reached the
 * container as an unhandled exception and the caller got a <b>500</b>. Every customer returning
 * after their week-old session expired hit a server error rather than a login prompt, and every
 * forged token produced one too — which also turns a probe into a reliable signal that a token
 * was rejected for a specific reason.
 *
 * <p>Reordering the two calls would have fixed the symptom and left the hazard: two methods
 * that must be called in one order, with nothing to say so. {@link AccessTokens#subjectOf}
 * replaced both. There is no wrong order available here now, and an unreadable token — expired,
 * unsigned, signed with someone else's key, or simply not a JWT — takes the same path as no
 * token at all: the request continues unauthenticated, and the security chain answers 401 or
 * 403 as the matched rule requires.
 *
 * <p>A token whose subject no longer has an account is treated the same way. It used to throw
 * {@code UsernameNotFoundException} from inside the filter, which is the same 500 by a
 * different route — a customer whose account was deleted while they were signed in.
 */
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokens accessTokens;
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        accessTokens.subjectOf(authHeader.substring(7))
                .flatMap(users::findByEmail)
                .map(AuthenticatedUser::new)
                .ifPresent(principal -> {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null, principal.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }
}
