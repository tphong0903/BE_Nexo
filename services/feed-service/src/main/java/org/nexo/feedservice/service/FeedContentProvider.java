package org.nexo.feedservice.service;

import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

public interface FeedContentProvider<T> {
    FeedContentType type();

    Mono<List<T>> getByIds(List<Long> ids, Long viewerId);

    Comparator<T> newestFirst();
}
