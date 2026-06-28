package org.nexo.feedservice.util;

import lombok.experimental.UtilityClass;
import org.nexo.feedservice.service.FeedContentType;

@UtilityClass
public class RedisKeyUtil {
    public String personalFeed(FeedContentType type, Long userId) {
        return type.personalFeedPrefix() + userId;
    }

    public String userOutbox(FeedContentType type, Long authorId) {
        return type.outboxPrefix() + authorId;
    }

    public String trending(FeedContentType type) {
        return type.trendingKey();
    }

    public String affinity(Long followerId) {
        return "affinity:" + followerId;
    }
}
