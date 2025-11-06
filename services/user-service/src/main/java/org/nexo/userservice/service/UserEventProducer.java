package org.nexo.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.dto.UserSearchEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, UserSearchEvent> kafkaTemplate;

    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;

    public void sendUserEvent(UserSearchEvent event) {

        kafkaTemplate.send(userEventsTopic,
                event.getId().toString(), event);
    }
}
