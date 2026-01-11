package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.LoginResponseDTO;
import com.dominator.bookify.dto.UserResponseDTO;
import com.dominator.bookify.model.Address;
import com.dominator.bookify.model.User;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.security.JwtUtil;
import com.dominator.bookify.service.AddressService;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.auth.oauth2.TokenVerifier;
import com.google.api.client.json.webtoken.JsonWebToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class OAuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final TokenVerifier tokenVerifier;
    private final AddressService addressService;

    public record CredentialRequest(String credential) {}

    @PostMapping("/token")
    public LoginResponseDTO exchangeToken(@RequestBody CredentialRequest req) {
        JsonWebSignature jws;
        try {
            jws = tokenVerifier.verify(req.credential());
        } catch (TokenVerifier.VerificationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID-token", ex);
        }

        if (jws == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID-token");
        }

        JsonWebToken.Payload payload = jws.getPayload();
        String email = (String) payload.get("email");
        String name  = (String) payload.get("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFullName(name);
            u.setRole("CUSTOMER");
            u.setVerified(true);
            return userRepository.save(u);
        });

        String token = jwtUtil.generateToken(email);

        return new LoginResponseDTO(token, convertToUserDTO(user));
    }

    private UserResponseDTO convertToUserDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setProfileAvatar(user.getProfileAvatar());
        dto.setFullName(user.getFullName());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFavorites(user.getFavorites());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setVerified(user.isVerified());
        dto.setStatus(user.getStatus());
        dto.setAddress(user.getAddress());
        return dto;
    }
}
