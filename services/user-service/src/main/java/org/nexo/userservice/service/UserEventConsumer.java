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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final MeilisearchService meilisearchService;

    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUserEvent(Object payload) {
        if (!(payload instanceof java.util.Map<?, ?> map)) {
            return;
        }

        Object eventTypeObj = map.get("eventType");
        if (!(eventTypeObj instanceof String eventType)) {
            return;
        }

        if (!("CREATE".equals(eventType) || "UPDATE".equals(eventType) || "DELETE".equals(eventType))) {
            return;
        }

        UserSearchEvent event = UserSearchEvent.builder()
                .id(map.get("id") instanceof Number idNum ? idNum.longValue() : null)
                .username((String) map.getOrDefault("username", null))
                .fullName((String) map.getOrDefault("fullName", null))
                .email((String) map.getOrDefault("email", null))
                .avatar((String) map.getOrDefault("avatar", null))
                .bio((String) map.getOrDefault("bio", null))
                .isPrivate((Boolean) map.getOrDefault("isPrivate", null))
                .accountStatus((String) map.getOrDefault("accountStatus", null))
                .eventType(eventType)
                .role((String) map.getOrDefault("role", null))
                .violationCount(map.get("violationCount") instanceof Number v ? v.intValue() : null)
                .build();

        UserSearchDocument document = convertToDocument(event);
        UserResponseAdmin documentAdmin = convertToDocumentAdmin(event);
        try {
            switch (event.getEventType()) {
                case "CREATE":
                case "UPDATE":
                    meilisearchService.updateUser(document, documentAdmin);
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
