package org.nexo.messagingservice.service.Impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nexo.grpc.user.UserServiceProto.UserDTOResponse;
import org.nexo.messagingservice.dto.MessageDTO;
import org.nexo.messagingservice.dto.MessageMediaDTO;
import org.nexo.messagingservice.dto.ReactionDTO;
import org.nexo.messagingservice.dto.SendMessageRequest;
import org.nexo.messagingservice.dto.UserDTO;
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
    private final UserGrpcClient userClientService;

    public MessageDTO sendMessage(SendMessageRequest request, Long senderUserId) {
        ConversationModel conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

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
        UserDTOResponse sender = userClientService.getUserById(message.getSenderUserId());
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
}
