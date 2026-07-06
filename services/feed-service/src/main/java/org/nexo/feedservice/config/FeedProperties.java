package org.nexo.feedservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "feed")
public class FeedProperties {
    private int maxRedisFeedSize = 50;
    private long kolFollowerThreshold = 10_000L;
    private long affinityMultiplierSeconds = 3_600L;
    private int redisBatchConcurrency = 64;
}
