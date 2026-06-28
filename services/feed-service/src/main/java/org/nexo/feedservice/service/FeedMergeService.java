package org.nexo.feedservice.service;

import org.nexo.feedservice.dto.FeedItem;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FeedMergeService {

    public List<FeedItem> mergeSortedUnique(Collection<List<FeedItem>> sortedLists) {
        int estimatedSize = 0;
        for (List<FeedItem> list : sortedLists) {
            estimatedSize += list.size();
        }

        List<FeedItem> merged = new ArrayList<>(estimatedSize);
        Set<Long> seen = new HashSet<>(estimatedSize);
        PriorityQueue<Cursor> queue = new PriorityQueue<>();

        int listIndex = 0;
        for (List<FeedItem> list : sortedLists) {
            if (!list.isEmpty()) {
                queue.add(new Cursor(listIndex, 0, list.get(0)));
            }
            listIndex++;
        }

        List<List<FeedItem>> indexedLists = new ArrayList<>(sortedLists);
        while (!queue.isEmpty()) {
            Cursor cursor = queue.poll();
            addIfAbsent(merged, seen, cursor.item());

            int nextItemIndex = cursor.itemIndex() + 1;
            List<FeedItem> source = indexedLists.get(cursor.listIndex());
            if (nextItemIndex < source.size()) {
                queue.add(new Cursor(cursor.listIndex(), nextItemIndex, source.get(nextItemIndex)));
            }
        }

        return merged;
    }

    public List<Long> toItemIds(List<FeedItem> items) {
        List<Long> ids = new ArrayList<>(items.size());
        for (FeedItem item : items) {
            ids.add(item.getPostId());
        }
        return ids;
    }

    public List<Long> appendMissing(List<Long> primaryIds, List<Long> fallbackIds) {
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(primaryIds.size() + fallbackIds.size());
        uniqueIds.addAll(primaryIds);
        uniqueIds.addAll(fallbackIds);
        return new ArrayList<>(uniqueIds);
    }

    public List<Long> interleavePersonalAndTrending(List<Long> personalIds, List<Long> trendingIds, long startOffset, int limit) {
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(personalIds.size() + trendingIds.size());
        int personalIndex = 0;
        int trendingIndex = 0;

        while (personalIndex < personalIds.size() || trendingIndex < trendingIds.size()) {
            if (personalIndex < personalIds.size()) {
                uniqueIds.add(personalIds.get(personalIndex++));
            }
            if (personalIndex < personalIds.size()) {
                uniqueIds.add(personalIds.get(personalIndex++));
            }
            if (trendingIndex < trendingIds.size()) {
                uniqueIds.add(trendingIds.get(trendingIndex++));
            }
        }

        List<Long> page = new ArrayList<>(limit);
        long skipped = 0;
        for (Long id : uniqueIds) {
            if (skipped++ < startOffset) {
                continue;
            }
            page.add(id);
            if (page.size() == limit) {
                break;
            }
        }
        return page;
    }

    private void addIfAbsent(List<FeedItem> target, Set<Long> seen, FeedItem item) {
        if (seen.add(item.getPostId())) {
            target.add(item);
        }
    }

    private record Cursor(int listIndex, int itemIndex, FeedItem item) implements Comparable<Cursor> {
        @Override
        public int compareTo(Cursor other) {
            return other.item().getCreatedAt().compareTo(item.getCreatedAt());
        }
    }
}
