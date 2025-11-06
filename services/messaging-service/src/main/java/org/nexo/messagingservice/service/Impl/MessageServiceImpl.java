package org.nexo.messagingservice.service.Impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse2;
import org.nexo.messagingservice.dto.MessageDTO;
import org.nexo.messagingservice.dto.MessageMediaDTO;
import org.nexo.messagingservice.dto.ReactionDTO;
import org.nexo.messagingservice.dto.ReactionDetailDTO;
import org.nexo.messagingservice.dto.SendMessageRequest;
import org.nexo.messagingservice.dto.UserDTO;
import org.nexo.messagingservice.enums.EConversationStatus;
import org.nexo.messagingservice.enums.EMessageType;
import org.nexo.messagingservice.enums.EReactionType;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.model.ConversationModel;
import org.nexo.messagingservice.model.ConversationParticipantModel;
import org.nexo.messagingservice.model.MessageMediaModel;
import org.nexo.messagingservice.model.MessageModel;
import org.nexo.messagingservice.model.MessageReactionModel;
import org.nexo.messagingservice.repository.ConversationParticipantRepository;
import org.nexo.messagingservice.repository.ConversationRepository;
import org.nexo.messagingservice.repository.MessageMediaRepository;
import org.nexo.messagingservice.repository.MessageReactionRepository;
import org.nexo.messagingservice.repository.MessageRepository;
import org.nexo.messagingservice.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageMediaRepository mediaRepository;
    private final UserGrpcClient userGrpcClient;

    public MessageDTO sendMessage(SendMessageRequest request, Long senderUserId) {
        ConversationModel conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        List<Long> participantIds = participantRepository
                .findActiveUserIdsByConversationId(conversation.getId());

        Long recipientUserId = participantIds.stream()
                .filter(id -> !id.equals(senderUserId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        UserServiceProto.CheckIfBlockedResponse blockResponse = userGrpcClient.checkIfBlocked(senderUserId,
                recipientUserId);

        if (blockResponse.getIsBlocked()) {
            Long blockedBy = blockResponse.getBlockedByUserId();

            if (blockedBy == recipientUserId) {
                throw new SecurityException("You cannot send messages to this user. They may have blocked you.");
            } else if (blockedBy == senderUserId) {
                throw new SecurityException("You have blocked this user. Unblock them to send messages.");
            }
        }

        if (conversation.getStatus() == EConversationStatus.DECLINED) {
            conversation.setStatus(EConversationStatus.PENDING);
        }
        if (!isParticipant(conversation.getId(), senderUserId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        MessageModel messageModel = MessageModel.builder()
                .conversation(conversation)
                .senderUserId(senderUserId)
                .content(request.getContent())
                .messageType(request.getMessageType() != null ? request.getMessageType() : EMessageType.TEXT)
                .build();

        if (request.getReplyToMessageId() != null) {
            MessageModel replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElse(null);
            messageModel.setReplyToMessage(replyTo);
        }

        messageModel = messageRepository.save(messageModel);

        conversation.setLastMessageId(messageModel.getId());
        conversation.setLastMessageAt(messageModel.getCreatedAt());
        conversationRepository.save(conversation);

        return mapToDto(messageModel);
    }

    public Page<MessageDTO> getMessages(Long conversationId, Pageable pageable, Long requestingUserId, String search) {
        if (!isParticipant(conversationId, requestingUserId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }
        if (search != null && !search.isEmpty()) {
            Page<MessageModel> messages = messageRepository
                    .searchMessagesByKeyword(conversationId, search, pageable);

            return messages.map(this::mapToDto);
        } else {
            Page<MessageModel> messages = messageRepository
                    .findByConversationIdAndIsActiveTrueOrderByCreatedAtDesc(conversationId, pageable);

            return messages.map(this::mapToDto);
        }

    }

    public void markAsRead(Long messageId, Long userId) {
        MessageModel message = messageRepository.findByIdAndIsActiveTrue(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (message.getSenderUserId().equals(userId)) {
            return;
        }

        ConversationParticipantModel participant = participantRepository
                .findByConversationIdAndUserId(message.getConversation().getId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (participant.getLastReadMessageId() == null || messageId > participant.getLastReadMessageId()) {
            participant.setLastReadMessageId(messageId);
            participantRepository.save(participant);
        }
    }

    public void markConversationAsRead(Long conversationId, Long userId) {
        MessageModel latestMessage = messageRepository
                .findFirstByConversationIdAndIsActiveTrueOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (latestMessage != null) {
            markAsRead(latestMessage.getId(), userId);
        }
    }

    public Long getLastReadMessageId(Long conversationId, Long userId) {
        ConversationParticipantModel participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElse(null);

        return participant != null ? participant.getLastReadMessageId() : null;
    }

    public void deleteMessage(Long messageId, Long userId) {
        MessageModel message = messageRepository.findByIdAndIsActiveTrue(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getSenderUserId().equals(userId)) {
            throw new SecurityException("Only message sender can delete");
        }
        message.setIsActive(false);
        messageRepository.save(message);
        log.info("Message {} deleted by user {}", messageId, userId);
    }

    public void addReaction(Long messageId, Long userId, EReactionType reactionType) {
        MessageModel message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        MessageReactionModel existingReaction = reactionRepository
                .findByMessageIdAndUserId(messageId, userId)
                .orElse(null);

        if (existingReaction != null) {
            existingReaction.setReactionType(reactionType);
            reactionRepository.save(existingReaction);
            return;
        } else {
            log.info("Adding new reaction");

            MessageReactionModel reaction = new MessageReactionModel();
            reaction.setMessage(message);
            reaction.setUserId(userId);
            reaction.setReactionType(reactionType);

            reactionRepository.save(reaction);
        }

    }

    public void removeReaction(Long messageId, Long userId, EReactionType reactionType) {
        reactionRepository.deleteByMessageIdAndUserIdAndReactionType(messageId, userId, reactionType);
    }

    private boolean isParticipant(Long conversationId, Long userId) {
        return participantRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    private MessageDTO mapToDto(MessageModel message) {
        UserDTOResponse sender = userGrpcClient.getUserById(message.getSenderUserId());
        UserDTO senderUserDTO = UserDTO.builder()
                .id(sender.getId())
                .username(sender.getUsername())
                .fullName(sender.getFullName())
                .avatarUrl(sender.getAvatar())
                .build();
        List<MessageMediaModel> mediaModels = mediaRepository.findByMessageId(message.getId());
        List<MessageMediaDTO> mediaList = mediaModels.stream()
                .map(this::mapMediaToDto)
                .collect(Collectors.toList());

        List<MessageReactionModel> reactionModels = reactionRepository.findByMessageId(message.getId());
        List<ReactionDTO> reactions = aggregateReactions(reactionModels);

        MessageDTO replyToMessage = null;
        if (message.getReplyToMessage() != null) {
            replyToMessage = mapToDto(message.getReplyToMessage());
        }

        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sender(senderUserDTO)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .replyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null)
                .replyToMessage(replyToMessage)
                .mediaList(mediaList)
                .reactions(reactions)
                .createdAt(message.getCreatedAt())
                .build();
    }

    private MessageMediaDTO mapMediaToDto(MessageMediaModel media) {
        return MessageMediaDTO.builder()
                .id(media.getId())
                .mediaUrl(media.getMediaUrl())
                .mediaType(media.getMediaType())
                .build();
    }

    private List<ReactionDTO> aggregateReactions(List<MessageReactionModel> reactions) {
        Map<EReactionType, List<Long>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(
                        MessageReactionModel::getReactionType,
                        Collectors.mapping(MessageReactionModel::getUserId, Collectors.toList())));

        return grouped.entrySet().stream()
                .map(entry -> ReactionDTO.builder()
                        .reactionType(entry.getKey())
                        .count(entry.getValue().size())
                        .userIds(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ReactionDetailDTO> getMessageReactions(Long messageId, Long requestingUserId) {
        // Kiểm tra message có tồn tại không
        MessageModel message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Kiểm tra user có quyền xem message này không (phải là participant của
        // conversation)
        if (!isParticipant(message.getConversation().getId(), requestingUserId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        // Lấy tất cả reactions của message
        List<MessageReactionModel> reactions = reactionRepository.findByMessageId(messageId);

        // Lấy danh sách userId
        List<Long> userIds = reactions.stream()
                .map(MessageReactionModel::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // Lấy thông tin user từ gRPC
        List<UserDTOResponse2> users = userGrpcClient.getUsersByIds(userIds);

        // Map user info theo userId để dễ tìm kiếm
        Map<Long, UserDTOResponse2> userMap = users.stream()
                .collect(Collectors.toMap(UserDTOResponse2::getId, u -> u));

        // Map reactions sang DTO kèm user info
        return reactions.stream()
                .map(reaction -> {
                    UserDTOResponse2 user = userMap.get(reaction.getUserId());
                    return ReactionDetailDTO.builder()
                            .id(reaction.getId())
                            .userId(reaction.getUserId())
                            .username(user != null ? user.getUsername() : "Unknown")
                            .fullName(user != null ? user.getFullName() : "Unknown User")
                            .avatarUrl(user != null ? user.getAvatar() : null)
                            .reactionType(reaction.getReactionType())
                            .createdAt(reaction.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
