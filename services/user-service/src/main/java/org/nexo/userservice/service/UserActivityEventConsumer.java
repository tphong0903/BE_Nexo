package org.nexo.userservice.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.nexo.userservice.dto.UserActivityEvent;
import org.nexo.userservice.model.UserActivityLogModel;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.UserActivityLogRepository;
import org.nexo.userservice.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityEventConsumer {

    private final UserRepository userRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.user-activity-events}", groupId = "user-activity-group")
    public void consumeUserActivityEvent(Object incomingPayload) {
        Object payload = incomingPayload;
        if (payload instanceof org.apache.kafka.clients.consumer.ConsumerRecord) {
            payload = ((org.apache.kafka.clients.consumer.ConsumerRecord<?, ?>) payload).value();
        }
        log.info("Received activity event payload: class={}, value={}", payload != null ? payload.getClass().getName() : "null", payload);

        UserActivityEvent event = null;
        try {
            if (payload instanceof UserActivityEvent directEvent) {
                event = directEvent;
            } else {
                event = objectMapper.convertValue(payload, UserActivityEvent.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse UserActivityEvent payload: {}", e.getMessage(), e);
            return;
        }

        if (event == null || event.getEventType() == null || event.getUserId() == null) {
            log.warn("Skipping: eventType or userId is null. event={}", event);
            return;
        }

        String eventType = event.getEventType();
        if (!(  "POST_CREATED".equals(eventType) ||
                "POST_LIKED".equals(eventType) ||
                "COMMENT_CREATED".equals(eventType))) {
            log.info("Event type '{}' not handled by activity consumer, skipping", eventType);
            return;
        }

        Long userId = event.getUserId();
        UserModel user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User {} not found in DB, skipping event '{}'", userId, eventType);
            return;
        }

        String detailsJson = "{" +
                "\"targetId\":" + (event.getTargetId() != null ? event.getTargetId() : "null") + "," +
                "\"targetType\":\"" + (event.getTargetType() != null ? event.getTargetType() : "") + "\"," +
                "\"metadata\":\"" + (event.getMetadata() != null ? event.getMetadata() : "") + "\"" +
                "}";

        UserActivityLogModel logModel = UserActivityLogModel.builder()
                .user(user)
                .action(eventType)
                .detailsJson(detailsJson)
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();

        userActivityLogRepository.save(logModel);

        log.info("Saved UserActivityLog for userId={}, eventType={}", userId, eventType);
    }
}