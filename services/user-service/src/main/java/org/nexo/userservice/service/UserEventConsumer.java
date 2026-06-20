package org.nexo.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.nexo.userservice.dto.UserResponseAdmin;
import org.nexo.userservice.dto.UserSearchDocument;
import org.nexo.userservice.dto.UserSearchEvent;
import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.enums.ERole;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meilisearch.sdk.exceptions.MeilisearchException;

import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.model.UserModel;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final MeilisearchService meilisearchService;
    private final UserRepository userRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUserEvent(Object payload) {
        UserSearchEvent event = null;
        try {
            if (payload instanceof UserSearchEvent) {
                event = (UserSearchEvent) payload;
            } else if (payload instanceof String) {
                event = objectMapper.readValue((String) payload, UserSearchEvent.class);
            } else if (payload != null) {
                event = objectMapper.convertValue(payload, UserSearchEvent.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse user event payload", e);
            return;
        }

        if (event == null || event.getEventType() == null) {
            return;
        }

        String eventType = event.getEventType();
        if (!("CREATE".equals(eventType) || "UPDATE".equals(eventType) || "DELETE".equals(eventType) || "USER_DEACTIVATED".equals(eventType) || "USER_REACTIVATED".equals(eventType))) {
            return;
        }

        if (event.getId() == null) {
            // Check if payload was a map that had 'userId' instead of 'id'
            try {
                java.util.Map<?, ?> map = objectMapper.convertValue(payload, java.util.Map.class);
                if (map.get("userId") instanceof Number userIdNum) {
                    event.setId(userIdNum.longValue());
                }
            } catch (Exception ignored) {}
        }

        if ("USER_DEACTIVATED".equals(eventType)) {
            event.setAccountStatus("LOCKED");
        } else if ("USER_REACTIVATED".equals(eventType)) {
            event.setAccountStatus("ACTIVE");
        }

        UserSearchDocument document = convertToDocument(event);
        UserResponseAdmin documentAdmin = convertToDocumentAdmin(event);
        try {
            switch (event.getEventType()) {
                case "CREATE":
                case "UPDATE":
                    if ("LOCKED".equals(event.getAccountStatus())) {
                        if (event.getId() != null) {
                            meilisearchService.deactivateUser(event.getId(), documentAdmin);
                        }
                    } else {
                        meilisearchService.updateUser(document, documentAdmin);
                    }
                    break;
                case "USER_DEACTIVATED":
                    if (event.getId() != null) {
                        meilisearchService.deactivateUser(event.getId(), documentAdmin);
                    }
                    break;
                case "USER_REACTIVATED":
                    if (event.getId() != null) {
                        UserModel user = userRepository.findById(event.getId()).orElse(null);
                        if (user != null) {
                            document = UserSearchDocument.builder()
                                    .id(user.getId())
                                    .username(user.getUsername())
                                    .fullName(user.getFullName())
                                    .avatar(user.getAvatar())
                                    .build();
                            documentAdmin = UserResponseAdmin.builder()
                                    .id(user.getId())
                                    .username(user.getUsername())
                                    .fullName(user.getFullName())
                                    .email(user.getEmail())
                                    .accountStatus(EAccountStatus.ACTIVE)
                                    .role(user.getRole() != null ? user.getRole() : ERole.USER)
                                    .violationCount(user.getViolationCount())
                                    .build();
                        }
                        meilisearchService.updateUser(document, documentAdmin);
                    }
                    break;

                case "DELETE":
                    if (event.getId() != null) {
                        meilisearchService.deleteUser(event.getId());
                    }
                    break;
                default:
                    break;
            }
        } catch (JsonProcessingException | MeilisearchException e) {
            log.error("Error processing user event: {}", e.getMessage(), e);
        }
    }

    private UserSearchDocument convertToDocument(UserSearchEvent event) {
        return UserSearchDocument.builder()
                .id(event.getId())
                .username(event.getUsername())
                .fullName(event.getFullName())
                .avatar(event.getAvatar())
                .build();
    }

    private UserResponseAdmin convertToDocumentAdmin(UserSearchEvent event) {

        return UserResponseAdmin.builder()
                .id(event.getId())
                .username(event.getUsername())
                .fullName(event.getFullName())
                .email(event.getEmail())
                .accountStatus(EAccountStatus.fromString(event.getAccountStatus()))
                .role(event.getRole() != null ? Enum.valueOf(ERole.class, event.getRole()) : ERole.USER)
                .violationCount(event.getViolationCount())
                .build();
    }
}
