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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityEventConsumer {

    private final UserRepository userRepository;
    private final UserActivityLogRepository userActivityLogRepository;

    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUserActivityEvent(Object payload) {
        if (!(payload instanceof java.util.Map<?, ?> map)) {
            return;
        }

        Object eventTypeObj = map.get("eventType");
        Object userIdObj = map.get("userId");

        if (!(eventTypeObj instanceof String eventType) || !(userIdObj instanceof Number userIdNum)) {
            return;
        }

        if (!("POST_CREATED".equals(eventType) || "POST_LIKED".equals(eventType) || "COMMENT_CREATED".equals(eventType))) {
            return;
        }

        Long userId = userIdNum.longValue();
        UserModel user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        Object targetId = map.get("targetId");
        Object targetType = map.get("targetType");
        Object metadata = map.get("metadata");
        String detailsJson = "{" +
                "\"targetId\":" + (targetId != null ? String.valueOf(targetId) : "null") + "," +
                "\"targetType\":\"" + (targetType != null ? String.valueOf(targetType) : "") + "\"," +
                "\"metadata\":\"" + (metadata != null ? String.valueOf(metadata) : "") + "\"" +
                "}";

        UserActivityLogModel logModel = UserActivityLogModel.builder()
                .user(user)
                .action(eventType)
                .detailsJson(detailsJson)
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();

        userActivityLogRepository.save(logModel);
    }
}
