package com.example.backend.controllers;

import com.example.backend.models.*;
import com.example.backend.services.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    /**
     * Tạo nhóm mới
     * POST /api/v1/group
     */
    @PostMapping
    public ResponseEntity<GroupDto> createGroup(
            @Valid @RequestBody GroupRequest.Create request,
            Authentication currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.createGroup(request, currentUser));
    }

    /**
     * Lấy danh sách nhóm của tôi
     * GET /api/v1/group
     */
    @GetMapping
    public ResponseEntity<List<GroupDto>> getMyGroups(Authentication currentUser) {
        return ResponseEntity.ok(groupService.getMyGroups(currentUser));
    }

    /**
     * Lấy chi tiết nhóm
     * GET /api/v1/group/{groupId}
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto> getGroup(
            @PathVariable UUID groupId,
            Authentication currentUser) {
        return ResponseEntity.ok(groupService.getGroupById(groupId, currentUser));
    }

    /**
     * Cập nhật thông tin nhóm (chỉ admin)
     * PUT /api/v1/group/{groupId}
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<GroupDto> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupRequest.Update request,
            Authentication currentUser) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, request, currentUser));
    }

    /**
     * Thêm thành viên vào nhóm (chỉ admin)
     * POST /api/v1/group/{groupId}/members
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupDto> addMembers(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupRequest.AddMember request,
            Authentication currentUser) {
        return ResponseEntity.ok(groupService.addMembers(groupId, request, currentUser));
    }

    /**
     * Xóa thành viên khỏi nhóm (chỉ admin)
     * DELETE /api/v1/group/{groupId}/members/{userId}
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId,
            Authentication currentUser) {
        groupService.removeMember(groupId, userId, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Rời nhóm
     * DELETE /api/v1/group/{groupId}/leave
     */
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable UUID groupId,
            Authentication currentUser) {
        groupService.leaveGroup(groupId, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Gửi tin nhắn vào nhóm
     * POST /api/v1/group/{groupId}/messages
     */
    @PostMapping("/{groupId}/messages")
    public ResponseEntity<GroupMessageDto> sendMessage(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupRequest.SendMessage request,
            Authentication currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.sendMessage(groupId, request, currentUser));
    }

    /**
     * Lấy tin nhắn nhóm (phân trang)
     * GET /api/v1/group/{groupId}/messages?page=0&size=30
     */
    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<GroupMessageDto>> getMessages(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication currentUser) {
        return ResponseEntity.ok(groupService.getMessages(groupId, page, size, currentUser));
    }
}