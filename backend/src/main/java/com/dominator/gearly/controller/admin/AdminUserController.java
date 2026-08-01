package com.dominator.gearly.controller.admin;

import com.dominator.gearly.dto.AdminUserDTO;
import com.dominator.gearly.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserDTO>> getAllUsers(
            @RequestParam(value = "fullName_like", required = false) String fullNameLike,
            @RequestParam(value = "email", required = false) String emailLike
    ) {
        return ResponseEntity.ok(adminUserService.getAllUsers(fullNameLike, emailLike));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
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
