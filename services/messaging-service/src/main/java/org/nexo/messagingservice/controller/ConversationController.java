package org.nexo.messagingservice.controller;

import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.NicknameRequest;
import org.nexo.messagingservice.dto.ResponseData;
import org.nexo.messagingservice.service.Impl.ConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

}
