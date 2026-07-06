package org.nexo.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.notificationservice.dto.MessageDTO;
import org.nexo.notificationservice.dto.NotificationDTO;
import org.nexo.notificationservice.dto.PageModelResponse;
import org.nexo.notificationservice.dto.UserDTO;
import org.nexo.notificationservice.exception.CustomException;
import org.nexo.notificationservice.model.NotificationModel;
import org.nexo.notificationservice.repository.INotificationRepository;
import org.nexo.notificationservice.service.INotificationService;
import org.nexo.notificationservice.util.ENotificationType;
import org.nexo.notificationservice.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {
    private static final String UNREAD_COUNT_KEY = "noti:unread:count:";
    private static final String USER_CACHE_KEY = "user:profile:";
    private static final Long SYSTEM_ACTOR_ID = 0L;
    private static final String POST_REMOVED_MESSAGE = " Bài viết của bạn đã bị ẩn vì vi phạm Tiêu chuẩn cộng đồng.";
    private static final String COMMENT_REMOVED_MESSAGE = " Bình luận của bạn đã bị ẩn vì vi phạm Tiêu chuẩn cộng đồng.";

    private final INotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageModelResponse<?> getNotifications(Pageable pageable) {
        Long userId = securityUtil.getUserIdFromToken();
        Page<NotificationModel> notificationPage = notificationRepository.findByRecipientIdAndActorIdNotOrderByCreatedAtDesc(userId, userId, pageable);
        List<NotificationModel> rawNotifications = notificationPage.getContent();

        if (rawNotifications.isEmpty()) {
            return PageModelResponse.builder().content(List.of()).build();
        }

        List<Long> actorIds = rawNotifications.stream()
                .map(NotificationModel::getActorId)
                .filter(actorId -> !SYSTEM_ACTOR_ID.equals(actorId))
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDTO> userMap = getUsersWithCache(actorIds);

        Map<String, List<NotificationModel>> groupedNotifications = rawNotifications.stream()
                .collect(Collectors.groupingBy(notification -> {
                    if ((notification.getNotificationType() == ENotificationType.POST_REMOVED || (notification.getNotificationType() == ENotificationType.COMMENT_REMOVED))
                            && SYSTEM_ACTOR_ID.equals(notification.getActorId())) {
                        return "SYSTEM_" + notification.getId();
                    }

                    return notification.getTargetUrl() + "::" + notification.getNotificationType().name();
                }));

        List<NotificationDTO> finalDtoList = new ArrayList<>();
        for (List<NotificationModel> group : groupedNotifications.values()) {
            NotificationModel template = group.getFirst();

            List<UserDTO> usersInGroup = group.stream()
                    .map(notification -> getNotificationActor(notification, userMap))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (usersInGroup.isEmpty())
                continue;

            String dynamicMessage = generateDynamicMessage(usersInGroup, template.getNotificationType());

            boolean isGroupRead = group.stream().allMatch(NotificationModel::getIsRead);

            LocalDateTime latestTimestamp = group.stream()
                    .map(NotificationModel::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            finalDtoList.add(new NotificationDTO(
                    template.getId(),
                    template.getRecipientId(),
                    template.getNotificationType().name(),
                    template.getTargetUrl(),
                    dynamicMessage,
                    isGroupRead,
                    usersInGroup,
                    latestTimestamp
            ));
        }

        finalDtoList.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));

        return PageModelResponse.<NotificationDTO>builder()
                .pageNo(notificationPage.getNumber())
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .content(finalDtoList)
                .build();
    }

    @Override
    public Long getNotificationsUnread() {
        Long userId = securityUtil.getUserIdFromToken();
        String key = UNREAD_COUNT_KEY + userId;

        Integer cachedCount = (Integer) redisTemplate.opsForValue().get(key);
        if (cachedCount != null) {
            return cachedCount.longValue();
        }

        long count = notificationRepository.countByRecipientIdAndIsRead(userId, false);
        redisTemplate.opsForValue().set(key, (int) count, 10, TimeUnit.MINUTES);
        return count;
    }

    @Override
    public String readNotification(Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        NotificationModel model = notificationRepository.findById(id).orElse(null);
        if (model == null || !Objects.equals(model.getRecipientId(), userId))
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);

        if (!model.getIsRead()) {
            model.setIsRead(true);
            notificationRepository.save(model);
            decrementUnreadCache(userId);
        }
        return "Success";
    }

    @Override
    public String readAllNotification() {
        Long userId = securityUtil.getUserIdFromToken();
        List<NotificationModel> list = notificationRepository.findAllByRecipientIdAndIsReadOrderByCreatedAtDesc(userId, false);
        if (!list.isEmpty() && !Objects.equals(list.getFirst().getRecipientId(), userId)) {
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        }
        for (NotificationModel model : list) {
            model.setIsRead(true);
        }
        notificationRepository.saveAll(list);
        redisTemplate.delete(UNREAD_COUNT_KEY + userId);
        return "Success";
    }

    @Override
    public void readNotificationGroup(String targetUrl, String notificationType) {
        Long userId = securityUtil.getUserIdFromToken();
        ENotificationType type;

        try {
            type = ENotificationType.valueOf(notificationType);
        } catch (IllegalArgumentException e) {
            throw new CustomException("Notification type is not valid", HttpStatus.BAD_REQUEST);
        }

        List<NotificationModel> notificationsToUpdate = notificationRepository.findAllByRecipientIdAndTargetUrlAndNotificationTypeAndIsReadOrderByCreatedAtDesc(userId, targetUrl, type, false);

        if (!notificationsToUpdate.isEmpty()) {
            for (NotificationModel model : notificationsToUpdate) {
                model.setIsRead(true);
            }
            notificationRepository.saveAll(notificationsToUpdate);
        }
    }

    @KafkaListener(id = "notificationGroup", topics = "notification")
    public void listenNotificationMessage(MessageDTO messageDTO) {
        log.info("Received: {}", messageDTO.toString());

        ENotificationType notificationType = ENotificationType.valueOf(messageDTO.getNotificationType());
        boolean systemNotification = SYSTEM_ACTOR_ID.equals(messageDTO.getActorId());
        List<Long> userIds = systemNotification
                ? List.of(messageDTO.getRecipientId())
                : List.of(messageDTO.getActorId(), messageDTO.getRecipientId());

        List<UserServiceProto.UserDTOResponse2> listUser = userGrpcClient.getUsersByIds(userIds);

        UserServiceProto.UserDTOResponse2 actor = systemNotification ? null : listUser.stream()
                .filter(u -> Objects.equals(u.getId(), messageDTO.getActorId()))
                .findFirst()
                .orElse(null);

        UserServiceProto.UserDTOResponse2 recipient = listUser.stream()
                .filter(u -> Objects.equals(u.getId(), messageDTO.getRecipientId()))
                .findFirst()
                .orElse(null);

        if ((!systemNotification && actor == null) || recipient == null) {
            log.warn("Actor or recipient is not exist for message {}", messageDTO);
            return;
        }

        String message = switch (notificationType) {
            case LIKE_POST -> actor.getUsername() + " đã thích bài viết của bạn";
            case LIKE_STORY -> actor.getUsername() + " đã thích story của bạn";
            case LIKE_COMMENT -> actor.getUsername() + " đã thích bình luận của bạn";
            case LIKE_REEL -> actor.getUsername() + " đã thích reel của bạn";

            case COMMENT_POST -> actor.getUsername() + " đã bình luận vào bài viết của bạn";
            case COMMENT_REEL -> actor.getUsername() + " đã bình luận vào reel của bạn";
            case MENTION_COMMENT, COMMENT_MENTION -> "đã nhắc đến bạn trong một bình luận";

            case FOLLOW -> actor.getUsername() + " đã theo dõi bạn";
            case TAG -> actor.getUsername() + " đã gắn thẻ bạn trong một bài viết";
            case MESSAGE -> actor.getUsername() + " đã gửi cho bạn một tin nhắn";
            case POST_REMOVED -> POST_REMOVED_MESSAGE;
            case COMMENT_REMOVED -> COMMENT_REMOVED_MESSAGE;

            default -> "Có một thông báo mới";
        };
        Long actorId = systemNotification ? SYSTEM_ACTOR_ID : actor.getId();
        boolean existedNotification = notificationRepository.existsByRecipientIdAndActorIdAndMessageAndTargetUrl(
                recipient.getId(), actorId, message, messageDTO.getTargetUrl()
        );
        if ((notificationType != ENotificationType.POST_REMOVED && notificationType != ENotificationType.COMMENT_REMOVED) && existedNotification) {
            notificationRepository.deleteByRecipientIdAndActorIdAndMessageAndTargetUrl(
                    recipient.getId(),
                    actorId,
                    message,
                    messageDTO.getTargetUrl()
            );
            decrementUnreadCache(recipient.getId());
            log.info("Đã xóa thông báo tồn tại của {}: {}", recipient.getUsername(), message);
        } else {
            NotificationModel newModel = NotificationModel.builder()
                    .notificationType(ENotificationType.valueOf(messageDTO.getNotificationType()))
                    .targetUrl(messageDTO.getTargetUrl())
                    .isRead(false)
                    .actorId(actorId)
                    .recipientId(recipient.getId())
                    .message(message)
                    .build();
            notificationRepository.save(newModel);
            incrementUnreadCache(recipient.getId());
            NotificationDTO wsDto = new NotificationDTO(
                    newModel.getId(),
                    newModel.getRecipientId(),
                    newModel.getNotificationType().name(),
                    newModel.getTargetUrl(),
                    message,
                    false,
                    List.of(systemNotification ? getSystemUser() : new UserDTO(actor.getUsername(), actor.getAvatar())),
                    newModel.getCreatedAt()
            );

            log.info("==> [WEBSOCKET] Attempting to send message to user '{}'", recipient.getUsername());
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", wsDto);
            log.info("Notification sent to {}: {}", recipient.getUsername(), message);
        }
    }

    private String generateDynamicMessage(List<UserDTO> users, ENotificationType type) {
        if (type == ENotificationType.POST_REMOVED) return POST_REMOVED_MESSAGE;
        if (type == ENotificationType.COMMENT_REMOVED) return COMMENT_REMOVED_MESSAGE;

        int size = users.size();
        if (size == 0) return "Có thông báo mới.";

        String firstActorName = users.getFirst().getUserName();
        String actionText = switch (type) {
            case LIKE_POST -> "đã thích bài viết của bạn";
            case LIKE_STORY -> "đã thích story của bạn";
            case LIKE_COMMENT -> "đã thích bình luận của bạn";
            case LIKE_REEL -> "đã thích reel của bạn";

            case COMMENT_POST -> "đã bình luận vào bài viết của bạn";
            case COMMENT_REEL -> "đã bình luận vào reel của bạn";
            case MENTION_COMMENT, COMMENT_MENTION -> "đã nhắc đến bạn trong một bình luận";

            case FOLLOW -> "đã theo dõi bạn";
            case TAG -> "đã gắn thẻ bạn trong một bài viết";
            case MESSAGE -> "đã gửi cho bạn một tin nhắn";
            default -> "đã tương tác với bạn";
        };

        if (size == 1) {
            return " " + actionText;
        } else {
            return firstActorName + " và " + (size - 1) + " người khác " + actionText;
        }
    }

    private UserDTO getNotificationActor(NotificationModel notification, Map<Long, UserDTO> userMap) {
        if ((notification.getNotificationType() == ENotificationType.POST_REMOVED || notification.getNotificationType() == ENotificationType.COMMENT_REMOVED)
                && SYSTEM_ACTOR_ID.equals(notification.getActorId())) {
            return getSystemUser();
        }
        return userMap.get(notification.getActorId());
    }

    private UserDTO getSystemUser() {
        return new UserDTO("System", "https://res.cloudinary.com/dllwsmukj/image/upload/v1783144140/images_2_jyyjbo.jpg");
    }

    private void incrementUnreadCache(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().increment(key);
        }
    }

    private void decrementUnreadCache(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Long val = redisTemplate.opsForValue().decrement(key);
            if (val != null && val < 0) redisTemplate.opsForValue().set(key, 0);
        }
    }

    private Map<Long, UserDTO> getUsersWithCache(List<Long> actorIds) {
        Map<Long, UserDTO> result = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        for (Long id : actorIds) {
            UserDTO cached = (UserDTO) redisTemplate.opsForValue().get(USER_CACHE_KEY + id);
            if (cached != null) result.put(id, cached);
            else missingIds.add(id);
        }

        if (!missingIds.isEmpty()) {
            Map<Long, UserDTO> remoteUsers = userGrpcClient.getUsersByIds(missingIds).stream()
                    .collect(Collectors.toMap(
                            UserServiceProto.UserDTOResponse2::getId,
                            u -> new UserDTO(u.getUsername(), u.getAvatar())
                    ));

            remoteUsers.forEach((id, dto) -> {
                redisTemplate.opsForValue().set(USER_CACHE_KEY + id, dto, 1, TimeUnit.HOURS);
                result.put(id, dto);
            });
        }
        return result;
    }
}
