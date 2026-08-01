package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.LoginResponseDTO;
import com.dominator.gearly.service.user.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    public record CredentialRequest(String credential) {}

    @PostMapping("/token")
    public ResponseEntity<LoginResponseDTO> exchangeToken(@RequestBody CredentialRequest req) {
        return ResponseEntity.ok(oAuthService.exchangeToken(req.credential()));
    }
}
