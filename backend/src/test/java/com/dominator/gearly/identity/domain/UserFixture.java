package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Role;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link User} aggregates for tests, under the same two rules as {@code ProductFixture}
 * and {@code OrderFixture} — the ones that stop a fixture from quietly becoming the setter
 * surface the aggregate just lost:
 *
 * <ol>
 *   <li><b>State is reached through real behavior.</b> {@link Builder#verified} calls
 *       {@code User.verify()} and {@link Builder#favouring} calls {@code addFavorite}, so a
 *       fixture can only describe an account the production code could have produced.</li>
 *   <li><b>Reflection touches only the persistence-managed fields</b> — {@code id} and the two
 *       audit timestamps, which Spring Data populates on load and nothing else does.</li>
 * </ol>
 *
 * <p>A built user is "already registered": {@link Builder#build} drains the pending
 * {@link UserRegistered} so a test that saves one through a mocked repository does not have a
 * stray event waiting. {@link Builder#justRegistered} keeps it, for the tests that are about
 * the event.
 */
public final class UserFixture {

    /** The hasher tests use: reversible, obvious in a failure message, and not BCrypt-slow. */
    public static final PasswordHasher FAKE_HASHER = new PasswordHasher() {
        @Override
        public String hash(String rawPassword) {
            return "hashed(" + rawPassword + ")";
        }

        @Override
        public boolean matches(String rawPassword, String storedHash) {
            return hash(rawPassword).equals(storedHash);
        }
    };

    private UserFixture() {
    }

    public static Builder aUser() {
        return new Builder();
    }

    /** The shape most tests want: a verified, active customer with an id. */
    public static User customer(String id) {
        return aUser().withId(id).build();
    }

    public static final class Builder {

        private String id;
        private PersonName name = PersonName.of("Ada", "Lovelace");
        private String email = "ada@example.com";
        private String rawPassword = "secret";
        private String phone = "0123456789";
        private Address address;
        private Role role = Role.CUSTOMER;
        private boolean verified = true;
        private boolean active = true;
        private boolean keepRegistrationEvent = false;
        private final List<ProductId> favorites = new ArrayList<>();
        private Instant createdAt;
        private Instant updatedAt;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder named(String firstName, String lastName) {
            this.name = PersonName.of(firstName, lastName);
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withPassword(String rawPassword) {
            this.rawPassword = rawPassword;
            return this;
        }

        public Builder withPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder withoutPhone() {
            this.phone = null;
            return this;
        }

        public Builder at(Address address) {
            this.address = address;
            return this;
        }

        public Builder asAdmin() {
            this.role = Role.ADMIN;
            return this;
        }

        public Builder unverified() {
            this.verified = false;
            return this;
        }

        public Builder verified() {
            this.verified = true;
            return this;
        }

        public Builder inactive() {
            this.active = false;
            return this;
        }

        public Builder favouring(String... productIds) {
            for (String productId : productIds) {
                favorites.add(ProductId.of(productId));
            }
            return this;
        }

        /** Keeps the {@link UserRegistered} the factory raises, for tests about the event. */
        public Builder justRegistered() {
            this.keepRegistrationEvent = true;
            this.verified = false;
            return this;
        }

        public Builder persistedAs(String id, Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            User user = User.register(
                    name,
                    EmailAddress.of(email),
                    rawPassword,
                    FAKE_HASHER,
                    phone == null ? null : PhoneNumber.of(phone),
                    address);

            if (verified) {
                user.verify();
            }
            if (!active) {
                user.deactivate();
            }
            if (role == Role.ADMIN) {
                // The one field with no behavior behind it: roles are assigned out of band
                // (a seeded admin), never by a use case, so there is nothing to call.
                ReflectionTestUtils.setField(user, "role", Role.ADMIN);
            }
            favorites.forEach(user::addFavorite);

            if (!keepRegistrationEvent) {
                user.pullDomainEvents();
            }

            setPersistenceField(user, "id", id);
            setPersistenceField(user, "createdAt", createdAt);
            setPersistenceField(user, "updatedAt", updatedAt);
            return user;
        }

        private void setPersistenceField(User user, String field, Object value) {
            if (value != null) {
                ReflectionTestUtils.setField(user, field, value);
            }
        }
    }
}
