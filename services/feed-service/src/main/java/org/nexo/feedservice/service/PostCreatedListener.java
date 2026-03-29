package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.dto.MessagePostDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostCreatedListener {

    private final FeedService feedService;

    @KafkaListener(topics = "post-created", groupId = "feed-service-group")
    public void handlePostCreated(MessagePostDTO messageDTO) {
        try {
            log.info("Received message from Kafka (post-created): {}", messageDTO.getPostId());

            feedService.handleNewPost(messageDTO.getAuthorId(), messageDTO.getPostId(), messageDTO.getCreatedAt()).subscribe();

        } catch (Exception e) {
            log.error("Error while handling post-created event", e);
        }
    }

    @KafkaListener(topics = "reel-created", groupId = "feed-service-group")
    public void handleReelCreated(MessagePostDTO messageDTO) {
        try {
            log.info("Received message from Kafka (post-deleted): {}", messageDTO.getPostId());

            feedService.handleNewReel(messageDTO.getAuthorId(), messageDTO.getPostId(), messageDTO.getCreatedAt()).subscribe();

        } catch (Exception e) {
            log.error("Error while handling post-deleted event", e);
        }
    }
}
