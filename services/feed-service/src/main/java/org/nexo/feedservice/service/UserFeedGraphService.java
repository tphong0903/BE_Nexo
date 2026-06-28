package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.exception.GrpcFeedException;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFeedGraphService {
    private final UserGrpcClient userClient;

    public Mono<Long> countFollowerOfUser(Long userId) {
        return Mono.fromCallable(() -> userClient.countFollowerOfUser(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .transform(mono -> logLatency(mono, "grpc.countFollowerOfUser", 1))
                .onErrorMap(error -> new GrpcFeedException("Cannot count followers of user " + userId, error));
    }

    public Mono<List<Long>> getFanOutRecipientIds(Long authorId) {
        return Mono.fromCallable(() -> {
                    List<UserServiceProto.FolloweeInfo> followees =
                            userClient.getUserFollowees(authorId).getFolloweesList();
                    List<Long> recipientIds = new ArrayList<>(followees.size() + 1);
                    for (UserServiceProto.FolloweeInfo followee : followees) {
                        recipientIds.add(followee.getUserId());
                    }
                    recipientIds.add(authorId);
                    return recipientIds;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .transform(mono -> logLatency(mono, "grpc.getFanOutRecipientIds", 1))
                .onErrorMap(error -> new GrpcFeedException("Cannot query fan-out recipients", error));
    }

    public Mono<List<Long>> getFollowedKols(Long userId) {
        return Mono.fromCallable(() -> userClient.getFollowedKols(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .transform(mono -> logLatency(mono, "grpc.getFollowedKols", 1))
                .onErrorMap(error -> new GrpcFeedException("Cannot query followed KOLs", error));
    }

    private <T> Mono<T> logLatency(Mono<T> mono, String operation, int size) {
        return Mono.defer(() -> {
            long start = System.nanoTime();
            return mono.doFinally(signal -> log.debug("{} size={} latencyMs={}",
                    operation, size, (System.nanoTime() - start) / 1_000_000));
        });
    }
}
