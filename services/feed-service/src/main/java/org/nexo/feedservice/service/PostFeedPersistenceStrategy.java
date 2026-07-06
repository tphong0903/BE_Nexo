package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.model.FeedModel;
import org.nexo.feedservice.repository.IFeedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostFeedPersistenceStrategy implements FeedPersistenceStrategy {
    private final IFeedRepository feedRepository;

    @Override
    public FeedContentType type() {
        return FeedContentType.POST;
    }

    @Override
    public void saveFanOut(Long authorId, Long itemId, List<Long> recipientIds) {
        List<FeedModel> models = new ArrayList<>(recipientIds.size());
        for (Long recipientId : recipientIds) {
            models.add(FeedModel.builder()
                    .followerId(recipientId)
                    .postId(itemId)
                    .userId(authorId)
                    .build());
        }
        feedRepository.saveAll(models);
    }

    @Override
    public Page<Long> findItemIdsByFollowerId(Long followerId, Pageable pageable) {
        return feedRepository.findPostIdsByFollowerId(followerId, pageable);
    }
}
