package com.dominator.gearly.identity.api;

import com.dominator.gearly.identity.application.SignedIn;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.ProductId;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns the {@link User} aggregate into the two JSON shapes the frontends read. Was
 * {@code mapper.UserMapper}.
 *
 * <p><b>The wire format is unchanged.</b> {@code email} and {@code phone} are strings on the
 * response as they always were, and {@code favorites} an array of bare id strings — the value
 * objects the aggregate now holds are unwrapped here, which is the whole reason this class
 * exists rather than the entity being serialized directly.
 *
 * <p>Never exposes the password hash. That was true of {@code UserMapper} too, by the mapper
 * listing fields one at a time; it is true here for the same reason, and the field additionally
 * carries {@code @JsonIgnore} so that a future mapper written in a hurry cannot undo it.
 */
@Component
public class UserResponseMapper {

    /** Customer-facing profile view (login response, profile screen). */
    public UserResponseDTO toResponseDto(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setProfileAvatar(user.getProfileAvatar());
        dto.setFullName(user.getFullName());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFavorites(idsOf(user.getFavorites()));
        dto.setEmail(valueOf(user.getEmail()));
        dto.setPhone(valueOf(user.getPhone()));
        dto.setVerified(user.isVerified());
        dto.setStatus(user.getStatus());
        dto.setAddress(user.getAddress());
        return dto;
    }

    /** The login/profile-update response: a token and the account it belongs to. */
    public LoginResponseDTO toLoginResponse(SignedIn signedIn) {
        return new LoginResponseDTO(signedIn.token(), toResponseDto(signedIn.user()));
    }

    /** Admin console view: adds role and audit timestamps, omits the password hash. */
    public AdminUserDTO toAdminDto(User user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setEmail(valueOf(user.getEmail()));
        dto.setPhone(valueOf(user.getPhone()));
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole());
        dto.setVerified(user.isVerified());
        dto.setFavorites(idsOf(user.getFavorites()));
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setStatus(user.getStatus());
        return dto;
    }

    private static String valueOf(EmailAddress email) {
        return email == null ? null : email.value();
    }

    private static String valueOf(PhoneNumber phone) {
        return phone == null ? null : phone.value();
    }

    private static List<String> idsOf(List<ProductId> favorites) {
        return favorites == null ? List.of() : favorites.stream().map(ProductId::value).toList();
    }
}
