package com.dominator.gearly.identity.api;

import com.dominator.gearly.shared.api.MessageResponse;
import com.dominator.gearly.identity.application.AuthService;
import com.dominator.gearly.identity.application.RegisterUserCommand;
import com.dominator.gearly.identity.application.UserProfileService;
import com.dominator.gearly.identity.application.VerificationTokenService;
import com.dominator.gearly.identity.domain.VerificationToken;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.UserId;
import com.dominator.gearly.storage.domain.UploadedFile;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The customer-facing account endpoints. Same URLs, same bodies, same responses.
 *
 * <p>This is where the authenticated principal stops, as it does in {@code OrderController}:
 * every authenticated call below unwraps {@link AuthenticatedUser} into a {@link UserId} before
 * calling in, so no application service has a security type in its signature and every one of
 * them is constructible in a test with no security context.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final AuthService authService;
    private final VerificationTokenService verificationTokenService;
    private final UserResponseMapper userResponseMapper;

    /**
     * Where the browser lands after following a verification link.
     *
     * <p>Configuration rather than {@code "http://localhost:5173"} spelled into three redirects.
     * The hard-coded value made the verification mail unusable anywhere but a developer's
     * laptop: a link sent from a deployed backend redirected the recipient to their own machine.
     * S13 did the same for the backend URLs the mail itself carried — see
     * {@code identity.domain.VerificationLinks} and {@code gearly.identity.public-base-url}.
     */
    @Value("${gearly.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @PostMapping("/update")
    public ResponseEntity<LoginResponseDTO> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid UserUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(userResponseMapper.toLoginResponse(
                userProfileService.updateProfile(callerId(authUser),
                        new UserProfileService.UpdateProfileCommand(
                                request.getFirstName(),
                                request.getLastName(),
                                request.getEmail(),
                                request.getPhone(),
                                request.getAddress()))));
    }

    /**
     * The multipart request is adapted to the storage port's {@code UploadedFile} here, at the
     * edge — {@code MultipartFile} is a web type and has no business in a use case.
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<MessageResponse> uploadAvatar(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam("avatar") MultipartFile file
    ) {
        userProfileService.uploadAvatar(
                callerId(authUser),
                new UploadedFile(file.getContentType(), file.getSize(), file::getInputStream));
        return ResponseEntity.ok(new MessageResponse("Avatar uploaded successfully."));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateUser(@AuthenticationPrincipal AuthenticatedUser authUser) {
        userProfileService.deactivate(callerId(authUser));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid UserLoginRequestDTO req) {
        return ResponseEntity.ok(userResponseMapper.toLoginResponse(
                authService.login(req.getEmail(), req.getPassword())));
    }

    @PostMapping("/logout")
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid UserRegisterRequestDTO req) {
        authService.register(new RegisterUserCommand(
                req.getFirstName(),
                req.getLastName(),
                req.getEmail(),
                req.getPassword(),
                req.getPhone(),
                req.getStreetAddress(),
                req.getCity(),
                req.getState(),
                req.getPostalCode(),
                req.getCountry()));
        return ResponseEntity.ok(new MessageResponse(
                "Verification email sent, please check your email to verify your email address!"));
    }

    /**
     * Browser-facing verification link. Redirects back to the frontend on success or failure,
     * so it deliberately handles its own errors rather than delegating to GlobalExceptionHandler
     * (which returns JSON, not a redirect).
     */
    @GetMapping("/verify")
    public void verifyToken(
            @RequestParam String token,
            @RequestParam VerificationToken.TokenType tokenType,
            HttpServletResponse response
    ) throws IOException {
        try {
            verificationTokenService.verifyToken(token, tokenType);

            if (tokenType == VerificationToken.TokenType.EMAIL_VERIFICATION) {
                response.sendRedirect(frontendBaseUrl + "/login?verified=1");
            } else if (tokenType == VerificationToken.TokenType.PASSWORD_RESET) {
                response.sendRedirect(frontendBaseUrl + "/reset-password?token=" + token + "&verified=1");
            }
        } catch (Exception e) {
            String error = URLEncoder.encode(String.valueOf(e.getMessage()), StandardCharsets.UTF_8);
            response.sendRedirect(frontendBaseUrl + "/login?verified=0&error=" + error);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resend(@RequestBody @Valid EmailRequestDTO req) {
        verificationTokenService.resendVerification(req.getEmail());
        return ResponseEntity.ok(new MessageResponse("Verification email re-sent."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody @Valid EmailRequestDTO req) {
        authService.handleForgotPassword(req.getEmail());
        return ResponseEntity.ok(new MessageResponse("Password reset link sent."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid ChangePasswordRequestDTO req
    ) {
        authService.changePassword(callerId(authUser), req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password change successful."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password reset successful."));
    }

    private static UserId callerId(AuthenticatedUser authUser) {
        return authUser.id();
    }
}
