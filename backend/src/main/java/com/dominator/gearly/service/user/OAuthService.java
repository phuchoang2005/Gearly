package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.LoginResponseDTO;
import com.dominator.gearly.exception.UnauthorizedException;
import com.dominator.gearly.mapper.UserMapper;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.JwtUtil;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Verifies a Google ID token, upserts the corresponding user, and issues an app JWT.
 * Extracted from OAuthController so the controller stays a thin transport layer.
 */
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final TokenVerifier tokenVerifier;
    private final UserMapper userMapper;

    public LoginResponseDTO exchangeToken(String credential) {
        JsonWebSignature jws;
        try {
            jws = tokenVerifier.verify(credential);
        } catch (TokenVerifier.VerificationException ex) {
            throw new UnauthorizedException("Invalid Google ID-token");
        }

        if (jws == null) {
            throw new UnauthorizedException("Invalid Google ID-token");
        }

        JsonWebToken.Payload payload = jws.getPayload();
        String email = (String) payload.get("email");
        String name = (String) payload.get("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFullName(name);
            u.setRole("CUSTOMER");
            u.setVerified(true);
            return userRepository.save(u);
        });

        String token = jwtUtil.generateToken(email);
        return new LoginResponseDTO(token, userMapper.toResponseDto(user));
    }
}
