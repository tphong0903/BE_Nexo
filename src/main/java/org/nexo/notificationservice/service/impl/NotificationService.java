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
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {
    private final INotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public PageModelResponse<?> getNotifications(Pageable pageable) {
        Long userId = securityUtil.getUserIdFromToken();
        Page<NotificationModel> notificationPage = notificationRepository.getNotificationModelByRecipientId(userId, pageable);
        List<NotificationModel> rawNotifications = notificationPage.getContent();

        if (rawNotifications.isEmpty()) {
            return PageModelResponse.builder()
                    .pageNo(notificationPage.getNumber())
                    .pageSize(notificationPage.getSize())
                    .totalElements(notificationPage.getTotalElements())
                    .totalPages(notificationPage.getTotalPages())
                    .last(notificationPage.isLast())
                    .content(List.of())
                    .build();
        }

        List<Long> actorIds = rawNotifications.stream()
                .map(NotificationModel::getActorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDTO> userMap = userGrpcClient.getUsersByIds(actorIds).stream()
                .collect(Collectors.toMap(
                        UserServiceProto.UserDTOResponse2::getId,
                        userProto -> new UserDTO(userProto.getUsername(), userProto.getAvatar())
                ));

        Map<String, List<NotificationModel>> groupedNotifications = rawNotifications.stream()
                .collect(Collectors.groupingBy(
                        n -> n.getTargetUrl() + "::" + n.getNotificationType().name()
                ));

        List<NotificationDTO> finalDtoList = new ArrayList<>();
        for (List<NotificationModel> group : groupedNotifications.values()) {
            NotificationModel template = group.getFirst();

            List<UserDTO> usersInGroup = group.stream()
                    .map(notification -> userMap.get(notification.getActorId()))
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
        return notificationRepository.countByRecipientIdAndIsRead(userId, false);
    }

    @Override
    public String readNotification(Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        NotificationModel model = notificationRepository.findById(id).orElse(null);
        if (model == null || !Objects.equals(model.getRecipientId(), userId))
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        model.setIsRead(true);
        notificationRepository.save(model);
        return "Success";
    }

    @Override
    public String readAllNotification() {
        Long userId = securityUtil.getUserIdFromToken();
        List<NotificationModel> list = notificationRepository.getAllByRecipientIdAndIsRead(userId, false);
        if (!list.isEmpty() && !Objects.equals(list.getFirst().getRecipientId(), userId)) {
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        }
        for (NotificationModel model : list) {
            model.setIsRead(true);
        }
        notificationRepository.saveAll(list);
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

        List<NotificationModel> notificationsToUpdate = notificationRepository.findAllByRecipientIdAndTargetUrlAndNotificationTypeAndIsRead(userId, targetUrl, type, false);

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

        List<UserServiceProto.UserDTOResponse2> listUser = userGrpcClient.getUsersByIds(
                List.of(messageDTO.getActorId(), messageDTO.getRecipientId())
        );

        UserServiceProto.UserDTOResponse2 actor = listUser.stream()
                .filter(u -> u.getId() == messageDTO.getActorId())
                .findFirst()
                .orElse(null);

        UserServiceProto.UserDTOResponse2 recipient = listUser.stream()
                .filter(u -> u.getId() == messageDTO.getRecipientId())
                .findFirst()
                .orElse(null);

        if (actor == null || recipient == null) {
            log.warn("Actor or recipient is not exist for message {}", messageDTO);
            return;
        }

        String message = switch (ENotificationType.valueOf(messageDTO.getNotificationType())) {
            case LIKE_POST -> actor.getUsername() + " đã thích bài viết của bạn";
            case LIKE_STORY -> actor.getUsername() + " đã thích story của bạn";
            case LIKE_COMMENT -> actor.getUsername() + " đã thích bình luận của bạn";
            case LIKE_REEL -> actor.getUsername() + " đã thích reel của bạn";

            case COMMENT_POST -> actor.getUsername() + " đã bình luận vào bài viết của bạn";
            case COMMENT_REEL -> actor.getUsername() + " đã bình luận vào reel của bạn";
            case COMMENT_MENTION -> actor.getUsername() + " đã nhắc đến bạn trong một bình luận";

            case FOLLOW -> actor.getUsername() + " đã theo dõi bạn";
            case TAG -> actor.getUsername() + " đã gắn thẻ bạn trong một bài viết";
            case MESSAGE -> actor.getUsername() + " đã gửi cho bạn một tin nhắn";

            default -> "Có một thông báo mới";
        };

        notificationRepository.save(NotificationModel.builder()
                .notificationType(ENotificationType.valueOf(messageDTO.getNotificationType()))
                .targetUrl(messageDTO.getTargetUrl())
                .isRead(false)
                .actorId(actor.getId())
                .recipientId(recipient.getId())
                .message(message)
                .build());

        messagingTemplate.convertAndSendToUser(String.valueOf(recipient.getId()), "/queue/notifications", message);

        log.info("Notification sent to {}: {}", recipient.getUsername(), message);
    }

    private String generateDynamicMessage(List<UserDTO> users, ENotificationType type) {
        int size = users.size();
        if (size == 0) return "Có thông báo mới."; // Fallback

        String firstActorName = users.getFirst().getUserName();
        String actionText = switch (type) {
            case LIKE_POST -> "đã thích bài viết của bạn";
            case LIKE_STORY -> "đã thích story của bạn";
            case LIKE_COMMENT -> "đã thích bình luận của bạn";
            case LIKE_REEL -> "đã thích reel của bạn";

            case COMMENT_POST -> "đã bình luận vào bài viết của bạn";
            case COMMENT_REEL -> "đã bình luận vào reel của bạn";
            case COMMENT_MENTION -> "đã nhắc đến bạn trong một bình luận";

            case FOLLOW -> "đã theo dõi bạn";
            case TAG -> "đã gắn thẻ bạn trong một bài viết";
            case MESSAGE -> "đã gửi cho bạn một tin nhắn";
            default -> "đã tương tác với bạn";
        };

        if (size == 1) {
            return firstActorName + " " + actionText;
        } else {
            return firstActorName + " và " + (size - 1) + " người khác " + actionText;
        }
    }
}
