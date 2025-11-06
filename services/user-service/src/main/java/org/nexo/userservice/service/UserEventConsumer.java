package org.nexo.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.dto.UserSearchDocument;
import org.nexo.userservice.dto.UserSearchEvent;
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
    public void consumeUserEvent(UserSearchEvent event) {

        UserSearchDocument document = convertToDocument(event);
        try {
            switch (event.getEventType()) {
                case "CREATE":
                case "UPDATE":
                    meilisearchService.updateUser(document);
                    break;

                case "DELETE":
                    meilisearchService.deleteUser(event.getId());
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
}
