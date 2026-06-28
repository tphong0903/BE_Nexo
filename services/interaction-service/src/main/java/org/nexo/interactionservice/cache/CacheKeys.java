package org.nexo.interactionservice.cache;

public final class CacheKeys {
    private CacheKeys() {
    }

    public static String post(Long id) {
        return "post:" + id;
    }

    public static String reel(Long id) {
        return "reel:" + id;
    }

    public static String comment(Long id) {
        return "comment:" + id;
    }

    public static String commentsVersion(String ownerType, Long ownerId) {
        return ownerType + ":" + ownerId + ":comments:version";
    }

    public static String likesVersion(String ownerType, Long ownerId) {
        return ownerType + ":" + ownerId + ":likes:version";
    }

    public static String repliesVersion(Long commentId) {
        return "comment:" + commentId + ":replies:version";
    }

    public static String commentsPage(String ownerType, Long ownerId, long version, int pageNo, int pageSize) {
        return ownerType + ":" + ownerId + ":comments:v:" + version + ":p:" + pageNo + ":s:" + pageSize;
    }

    public static String repliesPage(Long commentId, long version, int pageNo, int pageSize) {
        return "comment:" + commentId + ":replies:v:" + version + ":p:" + pageNo + ":s:" + pageSize;
    }

    public static String likesPage(String ownerType, Long ownerId, long version, int pageNo, int pageSize) {
        return ownerType + ":" + ownerId + ":likes:v:" + version + ":p:" + pageNo + ":s:" + pageSize;
    }

    public static String likeMembers(String ownerType, Long ownerId) {
        return ownerType + ":" + ownerId + ":likes";
    }

    public static String counters(String ownerType, Long ownerId) {
        return ownerType + ":" + ownerId + ":counters";
    }

    public static String userCounters(Long userId) {
        return "user:" + userId + ":statistics";
    }

    public static String globalCounters() {
        return "global:statistics";
    }

    public static String lock(String cacheKey) {
        return "lock:" + cacheKey;
    }
}
