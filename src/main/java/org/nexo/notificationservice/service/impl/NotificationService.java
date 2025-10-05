package org.nexo.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.notificationservice.dto.MessageDTO;
import org.nexo.notificationservice.dto.PageModelResponse;
import org.nexo.notificationservice.exception.CustomException;
import org.nexo.notificationservice.model.NotificationModel;
import org.nexo.notificationservice.repository.INotificationRepository;
import org.nexo.notificationservice.service.INotificationService;
import org.nexo.notificationservice.util.ENotificationType;
import org.nexo.notificationservice.util.SecurityUtil;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return null;
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
        if (model.getRecipientId() != userId)
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        model.setIsRead(true);
        notificationRepository.save(model);
        return "Success";
    }

    @Override
    public String readAllNotification() {
        Long userId = securityUtil.getUserIdFromToken();
        List<NotificationModel> list = notificationRepository.getAllByRecipientIdAndIsRead(userId, false);
        if (!list.isEmpty() && list.get(0).getRecipientId() != userId) {
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        }
        for (NotificationModel model : list) {
            model.setIsRead(true);
        }
        notificationRepository.saveAll(list);
        return "Success";
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

            case FOLLOW -> actor.getUsername() + " đã theo dõi bạn";
            case TAG -> actor.getUsername() + " đã gắn thẻ bạn trong một bài viết";
            case MESSAGE -> actor.getUsername() + " đã gửi cho bạn một tin nhắn";

            default -> "Có một thông báo mới";
        };

        notificationRepository.save(NotificationModel.builder()
                .notificationType(ENotificationType.valueOf(messageDTO.getNotificationType()))
                .targetUrl("Chua lam")
                .isRead(false)
                .actorId(actor.getId())
                .recipientId(recipient.getId())
                .message(message)
                .build());

        messagingTemplate.convertAndSendToUser(String.valueOf(recipient.getId()), "/queue/notifications", message);

        log.info("Notification sent to {}: {}", recipient.getUsername(), message);
    }

}
