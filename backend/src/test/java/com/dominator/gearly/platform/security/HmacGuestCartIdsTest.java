package com.dominator.gearly.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The guest-cart id signing, on its own.
 *
 * <p>What this has to establish is narrow and worth stating: a value the server issued comes
 * back with the same UUID inside it, and <b>nothing else does</b>. The second half is the
 * security property — before S12 every string was a valid guest id, so the interesting
 * assertions here are all rejections.
 */
@DisplayName("a guest cart id is only accepted if this server signed it")
class HmacGuestCartIdsTest {

    private static final String SECRET = "test-jwt-secret-key-that-is-long-enough-32bytes";

    private final HmacGuestCartIds ids = new HmacGuestCartIds(SECRET);

    @Test
    void anIssuedIdVerifiesAndUnwrapsToAUuid() {
        String issued = ids.issue();

        assertThat(ids.verify(issued)).hasValueSatisfying(raw -> {
            assertThat(UUID.fromString(raw)).hasToString(raw);   // a real UUID, not the token
            assertThat(issued).startsWith(raw + ".");
        });
    }

    @Test
    void everyIssuedIdIsDifferent() {
        assertThat(ids.issue()).isNotEqualTo(ids.issue());
    }

    /**
     * The case a returning visitor hits: {@code localStorage} still holds a bare UUID from
     * before ids were signed. Refusing it is what forces the storefront to re-init.
     */
    @Test
    void aBareUuidIsRefused() {
        assertThat(ids.verify(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void aForgedSignatureIsRefused() {
        String id = UUID.randomUUID().toString();

        assertThat(ids.verify(id + ".not-a-real-signature")).isEmpty();
    }

    @Test
    @DisplayName("swapping the id under a valid signature is refused")
    void aTamperedIdIsRefused() {
        String issued = ids.issue();
        String signature = issued.substring(issued.lastIndexOf('.') + 1);

        assertThat(ids.verify(UUID.randomUUID() + "." + signature)).isEmpty();
    }

    @Test
    @DisplayName("an id signed with a different key is refused")
    void anotherDeploymentsIdIsRefused() {
        String elsewhere = new HmacGuestCartIds("a-completely-different-secret-key-32bytes!").issue();

        assertThat(ids.verify(elsewhere)).isEmpty();
    }

    @Test
    void malformedValuesAreRefusedRatherThanThrowing() {
        assertThat(ids.verify(null)).isEmpty();
        assertThat(ids.verify("")).isEmpty();
        assertThat(ids.verify(".")).isEmpty();
        assertThat(ids.verify("no-separator")).isEmpty();
        assertThat(ids.verify("trailing-separator.")).isEmpty();
        assertThat(ids.verify(".leading-separator")).isEmpty();
    }
}
