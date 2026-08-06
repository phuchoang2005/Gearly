package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.shared.domain.AggregateRoot;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Role;
import com.dominator.gearly.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <b>The identity context's aggregate root.</b> A person with an account: who they are, how
 * they sign in, whether they are allowed to, and what they have saved.
 *
 * <h2>What changed</h2>
 * This was a Lombok {@code @Getter @Setter} bag with a fully public setter surface, and every
 * rule it owns lived in whichever service happened to be changing it:
 *
 * <ul>
 *   <li><b>Verification is a one-way door.</b> {@code setVerified(true)} was reachable from
 *       anywhere and reversible by anyone. It is {@link #verify()} now, and verifying an
 *       already-verified account is refused by the aggregate rather than by
 *       {@code VerificationTokenService} remembering to look.</li>
 *   <li><b>A password is only ever stored hashed.</b> Three call sites each did their own
 *       {@code setPasswordHash(encoder.encode(raw))}; a fourth that forgot the encode would
 *       have compiled. {@link #changePassword} takes a {@link PasswordHasher} and a raw
 *       password, so there is no way to reach the field with something unhashed.</li>
 *   <li><b>The favourites list has no duplicates.</b> {@code WishlistService} rebuilt a
 *       {@code HashSet} from the list on every mutation to get that property, in four places,
 *       and returned the list itself so a caller could hold a mutable handle on the
 *       aggregate's state.</li>
 * </ul>
 *
 * <h2>Why the wishlist stays here — a deliberate choice, reaffirmed</h2>
 * S11 recorded the reasoning at length ({@code WishlistService}): a set of ids with no
 * invariant of its own, never changed without its owner, never holding a catalog object. That
 * has not changed; what has is that the ids are {@link ProductId}s and the operations are
 * {@link #addFavorite}/{@link #removeFavorite} on the aggregate rather than four separate
 * read-modify-write sequences in a service. The size argument for splitting it out is still
 * open and still logged as a scaling follow-up.
 *
 * <h2>Persistence and the wire</h2>
 * Still a {@code @Document}, and the stored shape is byte-identical: {@code email} and
 * {@code phone} write as plain strings through the S9 converters, {@code favorites} as an array
 * of strings, {@code role} and {@code status} as their names. The entity is no longer
 * serialized to a client — {@code identity.api}'s DTOs are — but {@code passwordHash} carries
 * {@code @JsonIgnore} anyway, because the one thing that must never appear on the wire should
 * not depend on a mapper staying correct.
 */
@Getter
@Document(collection = "users")
public class User extends AggregateRoot {

    @Id
    private String id;

    private String profileAvatar;

    /**
     * The name parts and their derived display form, written together by {@link #rename} and
     * never individually — so {@code fullName} cannot drift away from the two fields it is
     * made of. That drift was real: {@code UserService} computed it from the parts while
     * {@code AuthService.register} stored whatever the registration body carried, leaving the
     * two disagreeing permanently with nothing to reconcile them. S9 closed it with
     * {@code setName(PersonName)}; the fix is the same, minus the setter.
     *
     * <p>All three stay stored separately because both frontends read {@code fullName} off
     * user responses.
     */
    private String firstName;

    private String lastName;

    private String fullName;

    @Indexed(unique = true)
    private EmailAddress email;

    /**
     * BCrypt output, never a raw password — see {@link #changePassword}. {@code @JsonIgnore}
     * is belt and braces: nothing serializes this entity any more.
     */
    @JsonIgnore
    private String passwordHash;

    private PhoneNumber phone;

    private Address address;

    private Role role = Role.DEFAULT;

    private boolean verified = false;

    /** Saved products. A set in meaning; a list on disk, because that is how it is stored. */
    private List<ProductId> favorites = new ArrayList<>();

    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** For Spring Data. */
    protected User() {
    }

    // ------------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------------

    /**
     * Someone signs up. Opens unverified, as a customer, active — and announces itself, so the
     * verification mail is sent by a listener after the registration has committed rather than
     * by the registration itself. See {@link UserRegistered}.
     */
    public static User register(PersonName name,
                                EmailAddress email,
                                String rawPassword,
                                PasswordHasher hasher,
                                PhoneNumber phone,
                                Address address) {
        User user = new User();
        user.rename(name);
        user.email = Objects.requireNonNull(email, "a user must have an email address");
        user.passwordHash = hasher.hash(rawPassword);
        user.phone = phone;
        user.address = address;
        user.role = Role.CUSTOMER;
        user.verified = false;
        user.status = UserStatus.ACTIVE;
        user.registerEvent(new UserRegistered(email, name, Instant.now()));
        return user;
    }

    /**
     * Someone signs in with Google for the first time.
     *
     * <p>Verified on arrival, because the identity provider has already done it, and with no
     * password — there is nothing to hash and no local credential to check. That is why this is
     * a separate factory rather than {@link #register} with nulls: an account with no password
     * is a legitimate state for exactly this path and a bug on any other, and the two are worth
     * telling apart at the point of creation.
     *
     * <p>Google returns one free-text {@code name} claim. It is split into parts when it has
     * them; a one-word display name is legal and keeps the previous behaviour of a full name
     * standing alone. No {@link UserRegistered} — the account is already verified, so there is
     * no mail to send.
     */
    public static User fromExternalIdentity(EmailAddress email, String displayName) {
        User user = new User();
        PersonName.tryParse(displayName).ifPresentOrElse(user::rename, () -> {
            user.fullName = displayName;
        });
        user.email = Objects.requireNonNull(email, "a user must have an email address");
        user.role = Role.CUSTOMER;
        user.verified = true;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    // ------------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------------

    /** The typed identity. Null until Mongo has assigned one on first save. */
    public UserId userId() {
        return id == null ? null : UserId.of(id);
    }

    /**
     * The stored parts as a value object, or {@code null} if this user has no name parts.
     *
     * <p>An externally-authenticated account can have a {@code fullName} and no parts, which is
     * why this is nullable and why {@link #displayName()} exists beside it.
     */
    @JsonIgnore
    public PersonName getName() {
        if (firstName == null || lastName == null) {
            return null;
        }
        return new PersonName(firstName, lastName);
    }

    /** What to show beside this person's reviews and orders. Never assembled by a caller. */
    public String displayName() {
        return fullName;
    }

    /** Read-only: favourites change through {@link #addFavorite}/{@link #removeFavorite}. */
    public List<ProductId> getFavorites() {
        return favorites == null ? List.of() : Collections.unmodifiableList(favorites);
    }

    @JsonIgnore
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean is(Role other) {
        return role == other;
    }

    /** Whether {@code rawPassword} is this account's password. */
    public boolean hasPassword(String rawPassword, PasswordHasher hasher) {
        return passwordHash != null && hasher.matches(rawPassword, passwordHash);
    }

    // ------------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------------

    /**
     * Consume an email-verification token.
     *
     * @throws AccountAlreadyVerifiedException if it has already been used
     */
    public void verify() {
        if (verified) {
            throw new AccountAlreadyVerifiedException();
        }
        verified = true;
    }

    /** Replace the credential. The raw password never reaches a field. */
    public void changePassword(String rawPassword, PasswordHasher hasher) {
        this.passwordHash = hasher.hash(rawPassword);
    }

    /** The customer closes their own account, or an administrator does. */
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /** The profile screen: name, contact details and address, together. */
    public void updateProfile(PersonName name, EmailAddress email, PhoneNumber phone, Address address) {
        rename(name);
        this.email = Objects.requireNonNull(email, "a user must have an email address");
        this.phone = phone;
        this.address = address;
    }

    public void changeAvatar(String publicPath) {
        this.profileAvatar = publicPath;
    }

    public void addFavorite(ProductId productId) {
        Set<ProductId> updated = currentFavorites();
        updated.add(Objects.requireNonNull(productId, "a favourite needs a product id"));
        this.favorites = new ArrayList<>(updated);
    }

    public void removeFavorite(ProductId productId) {
        Set<ProductId> updated = currentFavorites();
        updated.remove(productId);
        this.favorites = new ArrayList<>(updated);
    }

    /** Fold a guest's saved products into this account's, on sign-in. */
    public void addFavorites(List<ProductId> productIds) {
        Set<ProductId> updated = currentFavorites();
        updated.addAll(productIds);
        this.favorites = new ArrayList<>(updated);
    }

    public void removeFavorites(List<ProductId> productIds) {
        Set<ProductId> updated = currentFavorites();
        productIds.forEach(updated::remove);
        this.favorites = new ArrayList<>(updated);
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    /**
     * Sets the first name, the last name and the derived full name as one operation.
     * {@link PersonName#fullName()} is the only source of the third value.
     */
    private void rename(PersonName name) {
        Objects.requireNonNull(name, "a user must have a name");
        this.firstName = name.firstName();
        this.lastName = name.lastName();
        this.fullName = name.fullName();
    }

    /**
     * Insertion-ordered, so the wishlist page does not reshuffle itself on every change.
     * {@code WishlistService} used a plain {@code HashSet}, which reordered the whole list
     * each time an id was added.
     */
    private Set<ProductId> currentFavorites() {
        return favorites == null ? new LinkedHashSet<>() : new LinkedHashSet<>(favorites);
    }
}
