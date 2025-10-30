package org.nexo.messagingservice.controller;

import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.ResponseData;
import org.nexo.messagingservice.service.ConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

        @PutMapping("/{conversationId}/archive")
        public ResponseData<?> ArchiveConversation(
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
}
