package org.nexo.feedservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeedPersistenceStrategy {
    FeedContentType type();

    void saveFanOut(Long authorId, Long itemId, List<Long> recipientIds);

    Page<Long> findItemIdsByFollowerId(Long followerId, Pageable pageable);
}
