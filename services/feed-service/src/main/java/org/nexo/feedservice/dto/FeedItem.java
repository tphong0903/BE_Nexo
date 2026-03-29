package org.nexo.feedservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeedItem implements Comparable<FeedItem> {
    private Long postId;
    private Long createdAt;

    @Override
    public int compareTo(FeedItem other) {
        return other.createdAt.compareTo(this.createdAt);
    }
}