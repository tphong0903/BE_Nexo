package org.nexo.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.dto.RecommendationFollowedEvent;
import org.nexo.userservice.dto.RecommendationStatusEvent;
import org.nexo.userservice.dto.UserSearchEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;

    public void sendUserEvent(UserSearchEvent event) {
        kafkaTemplate.send(userEventsTopic, event.getId().toString(), event);
    }

    public void sendRecommendationFollowedEvent(RecommendationFollowedEvent event) {
        kafkaTemplate.send(userEventsTopic, String.valueOf(event.getFollowerId()), event);
    }

    public void sendRecommendationStatusEvent(RecommendationStatusEvent event) {
        kafkaTemplate.send(userEventsTopic, String.valueOf(event.getUserId()), event);
    }
}
