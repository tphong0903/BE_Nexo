package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.model.FeedReelModel;
import org.nexo.feedservice.repository.IFeedReelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReelFeedPersistenceStrategy implements FeedPersistenceStrategy {
    private final IFeedReelRepository feedReelRepository;

    @Override
    public FeedContentType type() {
        return FeedContentType.REEL;
    }

    @Override
    public void saveFanOut(Long authorId, Long itemId, List<Long> recipientIds) {
        List<FeedReelModel> models = new ArrayList<>(recipientIds.size());
        for (Long recipientId : recipientIds) {
            models.add(FeedReelModel.builder()
                    .followerId(recipientId)
                    .reelId(itemId)
                    .userId(authorId)
                    .build());
        }
        feedReelRepository.saveAll(models);
    }

    @Override
    public Page<Long> findItemIdsByFollowerId(Long followerId, Pageable pageable) {
        return feedReelRepository.findReelIdsByFollowerId(followerId, pageable);
    }
}
