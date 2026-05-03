package org.nexo.messagingservice.controller;

import org.nexo.messagingservice.dto.AddMembersRequest;
import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.CreateGroupRequest;
import org.nexo.messagingservice.dto.NicknameRequest;
import org.nexo.messagingservice.dto.ResponseData;
import org.nexo.messagingservice.dto.UpdateGroupRequest;
import org.nexo.messagingservice.service.ConversationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

        private final ConversationService conversationService;

        @GetMapping("/{recipientUserId}")
        public ResponseData<?> getOrCreateDirectConversation(
                        @PathVariable Long recipientUserId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();

                ConversationResponseDTO conversation = conversationService
                                .getOrCreateDirectConversation(keycloakUserId, recipientUserId);

                return ResponseData.builder()
                                .data(conversation)
                                .message("Direct conversation retrieved or created successfully")
                                .build();
        }

        @GetMapping
        public ResponseData<?> getConversations(
                        @RequestParam(value = "search", required = false) String search,
                        @PageableDefault(size = 10) Pageable pageable,
                        Authentication authentication) {
                String keycloakUserId = authentication.getName();
                return ResponseData.builder()
                                .data(conversationService.getUserConversations(keycloakUserId, pageable))
                                .message("User conversations retrieved successfully")
                                .build();
        }

        @GetMapping("/requests")
        public ResponseData<?> getConversationRequests(
                        @RequestParam(value = "search", required = false) String search,
                        @PageableDefault(size = 10) Pageable pageable,
                        Authentication authentication) {
                String keycloakUserId = authentication.getName();
                return ResponseData.builder()
                                .data(conversationService.getPendingRequests(keycloakUserId, pageable))
                                .message("User conversation requests retrieved successfully")
                                .build();
        }

        @GetMapping("/unread")
        public ResponseData<?> getUnreadConversations(
                        @PageableDefault(size = 10) Pageable pageable,
                        Authentication authentication) {
                String keycloakUserId = authentication.getName();
                return ResponseData.builder()
                                .data(conversationService.getUnreadConversations(keycloakUserId, pageable))
                                .message("Unread conversations retrieved successfully")
                                .build();
        }

        @GetMapping("/archived")
        public ResponseData<?> getArchivedConversations(
                        @PageableDefault(size = 10) Pageable pageable,
                        Authentication authentication) {
                String keycloakUserId = authentication.getName();
                return ResponseData.builder()
                                .data(conversationService.getArchivedConversations(keycloakUserId, pageable))
                                .message("Archived conversations retrieved successfully")
                                .build();
        }

        @PutMapping("/{conversationId}/archive")
        public ResponseData<?> archiveConversation(
                        @PathVariable Long conversationId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.archiveConversation(conversationId, keycloakUserId);

                return ResponseData.builder()
                                .message("Conversation archived successfully")
                                .build();
        }

        @PutMapping("/{conversationId}/mute")
        public ResponseData<?> muteConversation(
                        @PathVariable Long conversationId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.muteConversation(conversationId, keycloakUserId);

                return ResponseData.builder()
                                .message("Conversation muted successfully")
                                .build();

        }

        @PutMapping("/{conversationId}/decline")
        public ResponseData<?> declineConversation(
                        @PathVariable Long conversationId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.declineMessageRequest(conversationId, keycloakUserId);

                return ResponseData.builder()
                                .message("Conversation declined successfully")
                                .build();
        }

        @PutMapping("/{conversationId}/accept")
        public ResponseData<?> acceptConversation(
                        @PathVariable Long conversationId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.acceptMessageRequest(conversationId, keycloakUserId);

                return ResponseData.builder()
                                .message("Conversation accepted successfully")
                                .build();
        }

        @PutMapping("/{conversationId}/nickname")
        public ResponseData<?> setNickname(
                        @PathVariable Long conversationId,
                        @RequestBody NicknameRequest request,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();

                ConversationResponseDTO conversation = conversationService
                                .setNickname(conversationId, keycloakUserId, request);

                return ResponseData.builder()
                                .data(conversation)
                                .message("Nickname set successfully")
                                .build();
        }

        @GetMapping("/{conversationId}/nickname")
        public ResponseData<?> getNickname(
                        @PathVariable Long conversationId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();

                ConversationResponseDTO conversation = conversationService
                                .getNickname(conversationId, keycloakUserId);

                return ResponseData.builder()
                                .data(conversation)
                                .message("Nickname set successfully")
                                .build();
        }

        // ─── Group endpoints ──────────────────────────────────────────────────────

        @PostMapping("/group")
        public ResponseData<?> createGroup(
                        @RequestBody CreateGroupRequest request,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                ConversationResponseDTO group = conversationService.createGroup(keycloakUserId, request);

                return ResponseData.builder()
                                .data(group)
                                .message("Group created successfully")
                                .build();
        }

        @PutMapping("/{conversationId}/group")
        public ResponseData<?> updateGroup(
                        @PathVariable Long conversationId,
                        @RequestBody UpdateGroupRequest request,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                ConversationResponseDTO group = conversationService.updateGroup(conversationId, keycloakUserId, request);

                return ResponseData.builder()
                                .data(group)
                                .message("Group updated successfully")
                                .build();
        }

        @PostMapping("/{conversationId}/group/members")
        public ResponseData<?> addMembers(
                        @PathVariable Long conversationId,
                        @RequestBody AddMembersRequest request,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                ConversationResponseDTO group = conversationService.addMembers(conversationId, keycloakUserId, request);

                return ResponseData.builder()
                                .data(group)
                                .message("Members added successfully")
                                .build();
        }

        @DeleteMapping("/{conversationId}/group/members/{targetUserId}")
        public ResponseData<?> removeMember(
                        @PathVariable Long conversationId,
                        @PathVariable Long targetUserId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.removeMember(conversationId, keycloakUserId, targetUserId);

                return ResponseData.builder()
                                .message("Member removed successfully")
                                .build();
        }

        @PostMapping("/{conversationId}/group/leave")
        public ResponseData<?> leaveGroup(
                        @PathVariable Long conversationId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.leaveGroup(conversationId, keycloakUserId);

                return ResponseData.builder()
                                .message("Left group successfully")
                                .build();
        }

        @PutMapping("/{conversationId}/group/members/{targetUserId}/promote")
        public ResponseData<?> promoteAdmin(
                        @PathVariable Long conversationId,
                        @PathVariable Long targetUserId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.promoteAdmin(conversationId, keycloakUserId, targetUserId);

                return ResponseData.builder()
                                .message("Member promoted to admin")
                                .build();
        }

        @PutMapping("/{conversationId}/group/members/{targetUserId}/demote")
        public ResponseData<?> demoteAdmin(
                        @PathVariable Long conversationId,
                        @PathVariable Long targetUserId,
                        Authentication authentication) {

                String keycloakUserId = authentication.getName();
                conversationService.demoteAdmin(conversationId, keycloakUserId, targetUserId);

                return ResponseData.builder()
                                .message("Admin demoted to member")
                                .build();
        }

}
