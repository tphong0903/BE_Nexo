package org.nexo.feedservice.service;

public enum FeedContentType {
    POST("feed:", "user_posts:", "trending:posts"),
    REEL("feed:reel:", "user_reels:", "trending:reels");

    private final String personalFeedPrefix;
    private final String outboxPrefix;
    private final String trendingKey;

    FeedContentType(String personalFeedPrefix, String outboxPrefix, String trendingKey) {
        this.personalFeedPrefix = personalFeedPrefix;
        this.outboxPrefix = outboxPrefix;
        this.trendingKey = trendingKey;
    }

    public String personalFeedPrefix() {
        return personalFeedPrefix;
    }

    public String outboxPrefix() {
        return outboxPrefix;
    }

    public String trendingKey() {
        return trendingKey;
    }

    public static FeedContentType from(Boolean isPost) {
        return isPost ? POST : REEL;
    }
}
