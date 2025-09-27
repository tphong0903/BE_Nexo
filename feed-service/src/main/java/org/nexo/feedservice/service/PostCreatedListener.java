package org.nexo.feedservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PostCreatedListener {

    private final Flux<ReactiveSubscription.Message<String, String>> postCreatedStream;
    private final FeedService feedService;

    @PostConstruct
    public void subscribe() {
        postCreatedStream
                .map(ReactiveSubscription.Message::getMessage)
                .flatMap(this::handleMessage)
                .subscribe();
    }

    private Mono<Void> handleMessage(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(message);

            Long postId = node.get("postId").asLong();
            Long authorId = node.get("authorId").asLong();
            Long createdAt = node.get("createdAt").asLong();

            return feedService.handleNewPost(authorId, postId, createdAt);

        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}

