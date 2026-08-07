package com.dominator.gearly.identity.application;

import com.dominator.gearly.identity.domain.SignInRefusedException;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Verifies a Google ID token, upserts the corresponding account, and issues an app token.
 *
 * <p>The account creation is {@code User.fromExternalIdentity} now rather than six setters in a
 * lambda. The behaviour is the same — verified on arrival, customer role, name split into parts
 * when Google's single {@code name} claim has them — but it is one named path rather than a
 * shape assembled at the call site, which is what stopped it drifting away from
 * {@code register}'s idea of what a new account looks like.
 *
 * <p>An address that Google reports in a form {@code EmailAddress} refuses is a 401 rather than
 * a 500: the claim is external input, and a token we cannot make sense of is a token we do not
 * accept.
 */
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository users;
    private final AccessTokens accessTokens;
    private final TokenVerifier tokenVerifier;

    public SignedIn exchangeToken(String credential) {
        JsonWebSignature jws;
        try {
            jws = tokenVerifier.verify(credential);
        } catch (TokenVerifier.VerificationException ex) {
            throw SignInRefusedException.invalidGoogleToken();
        }

        if (jws == null) {
            throw SignInRefusedException.invalidGoogleToken();
        }

        JsonWebToken.Payload payload = jws.getPayload();
        EmailAddress email = emailOf((String) payload.get("email"));
        String name = (String) payload.get("name");

        User user = users.findByEmail(email)
                .orElseGet(() -> users.save(User.fromExternalIdentity(email, name)));

        return new SignedIn(accessTokens.issueFor(email), user);
    }

    private EmailAddress emailOf(String value) {
        try {
            return EmailAddress.of(value);
        } catch (IllegalArgumentException | NullPointerException malformed) {
            throw SignInRefusedException.invalidGoogleToken();
        }
    }
}
