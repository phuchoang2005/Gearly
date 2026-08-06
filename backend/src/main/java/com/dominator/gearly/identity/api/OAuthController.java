package com.dominator.gearly.identity.api;

import com.dominator.gearly.identity.application.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Exchanges a Google ID token for an app token. Unchanged URL and body. */
@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final UserResponseMapper userResponseMapper;

    public record CredentialRequest(String credential) {}

    @PostMapping("/token")
    public ResponseEntity<LoginResponseDTO> exchangeToken(@RequestBody CredentialRequest req) {
        return ResponseEntity.ok(userResponseMapper.toLoginResponse(
                oAuthService.exchangeToken(req.credential())));
    }
}
