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

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RedisExpirationListener(RedisMessageListenerContainer listenerContainer,
            KafkaTemplate<String, Object> kafkaTemplate) {
        super(listenerContainer);
        this.kafkaTemplate = kafkaTemplate;
        log.info("RedisExpirationListener initialized successfully");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (expiredKey.startsWith("story:expire:")) {
            String storyId = expiredKey.substring("story:expire:".length());
            try {
                kafkaTemplate.send("story-deletion-topic", new StoryDeletionEvent(Long.parseLong(storyId))).get();
                log.info("Published story ID [{}] to Kafka for deletion", storyId);
            } catch (Exception e) {
                log.error("Failed to publish story ID [{}] to Kafka", storyId, e);
            }
        } catch (Exception e) {
            log.error("Error processing Redis expiration message", e);
        }
    }
}