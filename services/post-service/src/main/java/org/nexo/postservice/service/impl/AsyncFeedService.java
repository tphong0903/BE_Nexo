package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.model.FeedModel;
import org.nexo.postservice.repository.IFeedRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsyncFeedService {

    private final IFeedRepository feedRepository;

    @Async
    public void saveFeedAsync(Long userId, List<UserServiceProto.FolloweeInfo> listFriend, Long postId) {
        List<FeedModel> list = new ArrayList<>();
        for (UserServiceProto.FolloweeInfo model : listFriend) {
            list.add(FeedModel.builder()
                    .userId(userId)
                    .postId(postId)
                    .followerId(model.getUserId())
                    .build());
        }
        feedRepository.saveAll(list);
    }
}
