package org.nexo.postservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.StoryDeletionEvent;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RedisExpirationListener extends KeyExpirationEventMessageListener {

    private final KafkaTemplate<String, StoryDeletionEvent> kafkaTemplate;

    public RedisExpirationListener(RedisMessageListenerContainer listenerContainer,
                                   KafkaTemplate<String, StoryDeletionEvent> kafkaTemplate) {
        super(listenerContainer);
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

            if (expiredKey.startsWith("story:expire:")) {
                String storyId = expiredKey.substring("story:expire:".length());

                kafkaTemplate.send("story-deletion-topic", new StoryDeletionEvent(Long.parseLong(storyId)))
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Published story ID [{}] to Kafka for deletion", storyId);
                            } else {
                                log.error("Failed to publish story ID [{}] to Kafka", storyId, ex);
                            }
                        });
            }
        } catch (Exception e) {
            log.error("Error processing Redis expiration message", e);
        }
    }
}