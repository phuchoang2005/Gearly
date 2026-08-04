package com.dominator.gearly.model;

import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String profileAvatar;

    /**
     * The name parts and their derived display form. Written together through
     * {@link #setName(PersonName)} — never individually — so {@code fullName} cannot drift
     * away from the two fields it is supposed to be made of. See that method for the bug
     * this closes.
     */
    private String firstName;

    private String lastName;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String phone;

    private Address address;

    private Role role = Role.DEFAULT;

    private boolean verified = false;

    private List<String> favorites; // Product IDs

    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Sets the first name, the last name and the derived full name as one operation.
     *
     * <p>This is the fix for a real split-brain. {@code UserService} computed
     * {@code fullName} from the parts on profile update, while {@code AuthService.register}
     * stored whatever {@code fullName} the registration body happened to carry — so
     * registering as "Jane Doe" with a {@code fullName} of "Bob" left the two disagreeing
     * permanently, with nothing to reconcile them. {@link PersonName#fullName()} is now the
     * only source of that value.
     *
     * <p>The three fields stay stored separately: S9 freezes the document shape, and both
     * frontends read {@code fullName} off user responses.
     */
    @JsonIgnore
    public void setName(PersonName name) {
        this.firstName = name.firstName();
        this.lastName = name.lastName();
        this.fullName = name.fullName();
    }

    /**
     * The stored parts as a value object, or {@code null} if this user has no name set.
     *
     * <p>{@code @JsonIgnore} on both accessors: they are a domain seam, not a field. Without
     * it Jackson would infer a {@code name} property and add it to every serialized user.
     */
    @JsonIgnore
    public PersonName getName() {
        if (firstName == null || lastName == null) {
            return null;
        }
        return new PersonName(firstName, lastName);
    }
}
