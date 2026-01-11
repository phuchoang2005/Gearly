package com.dominator.bookify.controller.admin;

import com.dominator.bookify.dto.*;
import com.dominator.bookify.service.admin.AdminBookService;
import com.dominator.bookify.service.admin.AdminUserService;
import com.dominator.bookify.service.user.BookService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(value = "fullName_like", required = false)String fullNameLike,
            @RequestParam(value = "email", required = false)String emailLike

    ){
        try {
            return ResponseEntity.ok().body(adminUserService.getAllUsers(fullNameLike, emailLike));
        }catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id){
        try {
            return ResponseEntity.ok().body(adminUserService.getUserById(id));
        }catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String id){
        try {
            boolean result = adminUserService.activateUser(id);
            return ResponseEntity.ok().body(result);
        }catch (ResponseStatusException e) {
            return ResponseEntity
                   .status(e.getStatusCode())
                   .body(Map.of("error", e.getReason()));
        }
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable String id){
        try {
            var result = adminUserService.deactivateUser(id);
            return ResponseEntity.ok().body(result);
        }catch (ResponseStatusException e) {
            return ResponseEntity
                   .status(e.getStatusCode())
                   .body(Map.of("error", e.getReason()));
        }
    }
}
