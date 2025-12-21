package org.nexo.messagingservice.service.Impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.grpc.user.UserServiceProto.CheckIfBlockedResponse;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse2;
import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.MessageDTO;
import org.nexo.messagingservice.dto.MessageMediaDTO;
import org.nexo.messagingservice.dto.ReactionDTO;
import org.nexo.messagingservice.dto.ReactionDetailDTO;
import org.nexo.messagingservice.dto.ReactionUpdateDTO;
import org.nexo.messagingservice.dto.ReplyStoryRequsestDTO;
import org.nexo.messagingservice.dto.SendMessageRequest;
import org.nexo.messagingservice.dto.UserDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.nexo.messagingservice.enums.EConversationStatus;
import org.nexo.messagingservice.enums.EMessageType;
import org.nexo.messagingservice.enums.EReactionType;
import org.nexo.messagingservice.grpc.StoryGrpcClient;
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
    private final StoryGrpcClient storyGrpcClient;
    private final SimpMessagingTemplate messagingTemplate;

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

        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            for (String url : request.getMediaUrls()) {
                MessageMediaModel media = new MessageMediaModel();
                media.setMessage(messageModel);
                media.setMediaUrl(url);
                media.setMediaType(determineMediaType(url));
                mediaRepository.save(media);
            }
        }

        conversation.setLastMessageId(messageModel.getId());
        conversation.setLastMessageAt(messageModel.getCreatedAt());
        conversationRepository.save(conversation);

        return mapToDto(messageModel);
    }

    public MessageDTO replyStory(ReplyStoryRequsestDTO request, Long senderUserId) {
        Long recipientUserId = request.getUserId();

        ConversationModel existing = conversationRepository.findFirstDirectConversationBetweenUsers(senderUserId,
                recipientUserId);
        if (existing != null) {
            ConversationModel conv = existing;
            if (conv.getStatus() == EConversationStatus.BLOCKED) {
                throw new SecurityException("Conversation is blocked");
            }

            if (conv.getStatus() == EConversationStatus.PENDING) {
                boolean areMutualFriends = userGrpcClient.areMutualFriends(senderUserId, recipientUserId);
                if (areMutualFriends) {
                    conv.setStatus(EConversationStatus.NORMAL);
                    conv = conversationRepository.save(conv);
                }
            }
            return mapToDto(sendMessage(senderUserId, recipientUserId, conv, request));
        }
        boolean areMutualFriends = userGrpcClient.areMutualFriends(senderUserId, recipientUserId);
        EConversationStatus initialStatus = areMutualFriends ? EConversationStatus.NORMAL : EConversationStatus.PENDING;

        ConversationModel conversation = ConversationModel.builder()
                .status(initialStatus)
                .build();
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation, senderUserId);
        addParticipant(conversation, recipientUserId);

        log.info("Created {} conversation between {} and {}", initialStatus, senderUserId, recipientUserId);

        return mapToDto(sendMessage(senderUserId, recipientUserId, conversation, request));
    }

    private MessageModel sendMessage(long senderUserId, long recipientUserId, ConversationModel conversation,
            ReplyStoryRequsestDTO request) {
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
                .storyId(request.getStoryId())
                .build();
        messageModel = messageRepository.save(messageModel);
        conversation.setLastMessageId(messageModel.getId());
        conversation.setLastMessageAt(messageModel.getCreatedAt());
        conversationRepository.save(conversation);
        return messageModel;
    }

    private void addParticipant(ConversationModel conversation, Long userId) {
        ConversationParticipantModel participant = new ConversationParticipantModel();
        participant.setConversation(conversation);
        participant.setUserId(userId);
        participantRepository.save(participant);
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
        } else {
            MessageReactionModel reaction = new MessageReactionModel();
            reaction.setMessage(message);
            reaction.setUserId(userId);
            reaction.setReactionType(reactionType);

            reactionRepository.save(reaction);
        }

        List<MessageReactionModel> reactionModels = reactionRepository.findByMessageId(messageId);
        List<ReactionDTO> reactions = aggregateReactions(reactionModels);
        ReactionUpdateDTO update = ReactionUpdateDTO.builder()
                .messageId(messageId)
                .reactions(reactions)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getId() + "/reactions",
                update);
    }

    public void removeReaction(Long messageId, Long userId, EReactionType reactionType) {
        MessageModel message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        reactionRepository.deleteByMessageIdAndUserIdAndReactionType(messageId, userId, reactionType);

        List<MessageReactionModel> reactionModels = reactionRepository.findByMessageId(messageId);
        List<ReactionDTO> reactions = aggregateReactions(reactionModels);
        ReactionUpdateDTO update = ReactionUpdateDTO.builder()
                .messageId(messageId)
                .reactions(reactions)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversation().getId() + "/reactions",
                update);
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
        if (message.getStoryId() != null) {
            String storyMediaUrl = storyGrpcClient.getStoryMediaIfActive(message.getStoryId());
            return MessageDTO.builder()
                    .id(message.getId())
                    .conversationId(message.getConversation().getId())
                    .status(message.getConversation().getStatus())
                    .sender(senderUserDTO)
                    .content(message.getContent())
                    .messageType(message.getMessageType())
                    .replyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null)
                    .replyToMessage(replyToMessage)
                    .mediaList(mediaList)
                    .reactions(reactions)
                    .createdAt(message.getCreatedAt())
                    .storyId(message.getStoryId())
                    .storyMediaUrl(storyMediaUrl)
                    .build();
        }
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .status(message.getConversation().getStatus())
                .sender(senderUserDTO)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .replyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null)
                .replyToMessage(replyToMessage)
                .mediaList(mediaList)
                .reactions(reactions)
                .createdAt(message.getCreatedAt())
                .storyId(null)
                .storyMediaUrl(null)
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
        MessageModel message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (!isParticipant(message.getConversation().getId(), requestingUserId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }
        List<MessageReactionModel> reactions = reactionRepository.findByMessageId(messageId);
        List<Long> userIds = reactions.stream()
                .map(MessageReactionModel::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<UserDTOResponse2> users = userGrpcClient.getUsersByIds(userIds);
        Map<Long, UserDTOResponse2> userMap = users.stream()
                .collect(Collectors.toMap(UserDTOResponse2::getId, u -> u));
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

    private EMessageType determineMediaType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png")
                || lowerUrl.endsWith(".gif") || lowerUrl.endsWith(".webp")) {
            return EMessageType.IMAGE;
        } else if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".avi") || lowerUrl.endsWith(".mov")
                || lowerUrl.endsWith(".mkv")) {
            return EMessageType.VIDEO;
        } else if (lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") || lowerUrl.endsWith(".aac")) {
            return EMessageType.AUDIO;
        } else {
            return EMessageType.FILE;
        }
    }
}
