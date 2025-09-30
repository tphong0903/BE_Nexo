package org.nexo.feedservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import reactor.core.publisher.Flux;

@Configuration
public class RedisSubscriberConfig {
    @Bean
    public ReactiveRedisMessageListenerContainer container(ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveRedisMessageListenerContainer(connectionFactory);
    }

    @Bean
    public Flux<ReactiveSubscription.Message<String, String>> postCreatedStream(
            ReactiveRedisMessageListenerContainer container,
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {

        return container.receive(ChannelTopic.of("post-created"))
                .doOnSubscribe(s -> System.out.println("Subscribed to post-created channel"))
                .share();
    }

}
