package com.dominator.gearly.identity.api;

import com.dominator.gearly.identity.application.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The admin console's account screens.
 *
 * <p>{@code @PreAuthorize("hasRole('ADMIN')")} on the class <em>in addition to</em> the
 * {@code /api/admin/**} URL rule in {@code SecurityConfig}, not instead of it. The URL rule is a
 * prefix match on a string and every endpoint that has ever escaped one did so by being mounted
 * somewhere the pattern did not reach — a controller moved, a mapping edited, a new method under
 * a path that looked similar. The annotation travels with the code.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserResponseMapper userResponseMapper;

    @GetMapping
    public ResponseEntity<List<AdminUserDTO>> getAllUsers(
            @RequestParam(value = "fullName_like", required = false) String fullNameLike,
            @RequestParam(value = "email", required = false) String emailLike
    ) {
        return ResponseEntity.ok(adminUserService.getAllUsers(fullNameLike, emailLike).stream()
                .map(userResponseMapper::toAdminDto)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userResponseMapper.toAdminDto(adminUserService.getUserById(id)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Boolean> activateUser(@PathVariable String id) {
        return ResponseEntity.ok(adminUserService.activateUser(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Boolean> deactivateUser(@PathVariable String id) {
        return ResponseEntity.ok(adminUserService.deactivateUser(id));
    }
}
