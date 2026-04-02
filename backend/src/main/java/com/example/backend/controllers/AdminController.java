package com.example.backend.controllers;

import com.example.backend.models.AdminGroupDto;
import com.example.backend.models.AdminStatsDto;
import com.example.backend.models.AdminUserDto;
import com.example.backend.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class AdminController {

    private final AdminService adminService;

    // ─── Quản lý User ────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }

    @PatchMapping("/users/{userId}/ban")
    public ResponseEntity<AdminUserDto> banUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.banUser(userId));
    }

    @PatchMapping("/users/{userId}/unban")
    public ResponseEntity<AdminUserDto> unbanUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.unbanUser(userId));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/{userId}/promote")
    public ResponseEntity<AdminUserDto> promoteToAdmin(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.promoteToAdmin(userId));
    }

    @PatchMapping("/users/{userId}/demote")
    public ResponseEntity<AdminUserDto> demoteToUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.demoteToUser(userId));
    }

    // ─── Quản lý Group ───────────────────────────────────────────────────────

    @GetMapping("/groups")
    public ResponseEntity<Page<AdminGroupDto>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllGroups(page, size));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId) {
        adminService.deleteGroup(groupId);
        return ResponseEntity.ok().build();
    }

    // ─── Thống kê ─────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
