package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.*;
import com.dominator.bookify.model.VerificationToken;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.service.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/update")
    public ResponseEntity<LoginResponseDTO> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid UserUpdateRequestDTO userUpdateRequestDTO
    ) {
        return ResponseEntity.ok(userService.updateProfile(authUser, userUpdateRequestDTO));
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<MessageResponse> uploadAvatar(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam("avatar") MultipartFile file
    ) throws IOException {
        userService.uploadAvatar(authUser, file);
        return ResponseEntity.ok(new MessageResponse("Avatar uploaded successfully."));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateUser(@AuthenticationPrincipal AuthenticatedUser authUser) {
        userService.deactiveUser(authUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid UserLoginRequestDTO req) {
        return ResponseEntity.ok(userService.login(req));
    }

    @PostMapping("/logout")
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody @Valid UserRegisterRequestDTO req) {
        userService.register(req);
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
            userService.verifyToken(token, tokenType);

            if (tokenType == VerificationToken.TokenType.EMAIL_VERIFICATION) {
                response.sendRedirect("http://localhost:5173/login?verified=1");
            } else if (tokenType == VerificationToken.TokenType.PASSWORD_RESET) {
                response.sendRedirect("http://localhost:5173/reset-password?token=" + token + "&verified=1");
            }
        } catch (Exception e) {
            String error = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect("http://localhost:5173/login?verified=0&error=" + error);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resend(@RequestBody @Valid EmailRequestDTO req) {
        userService.resendVerification(req.getEmail());
        return ResponseEntity.ok(new MessageResponse("Verification email re-sent."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody @Valid EmailRequestDTO req) {
        userService.handleForgotPassword(req.getEmail());
        return ResponseEntity.ok(new MessageResponse("Password reset link sent."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid ChangePasswordRequestDTO req
    ) {
        userService.changePassword(authUser, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password change successful."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO req) {
        userService.resetPassword(req);
        return ResponseEntity.ok(new MessageResponse("Password reset successful."));
    }
}
