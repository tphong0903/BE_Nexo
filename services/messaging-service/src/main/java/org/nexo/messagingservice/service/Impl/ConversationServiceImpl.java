package org.nexo.messagingservice.service.Impl;

import java.util.List;
import java.util.Optional;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse2;
import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.MessageDTO;
import org.nexo.messagingservice.dto.PageModelResponse;
import org.nexo.messagingservice.dto.UserDTO;
import org.nexo.messagingservice.exception.ResourceNotFoundException;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.model.ConversationModel;
import org.nexo.messagingservice.model.ConversationParticipantModel;
import org.nexo.messagingservice.model.MessageModel;
import org.nexo.messagingservice.repository.ConversationParticipantRepository;
import org.nexo.messagingservice.repository.ConversationRepository;
import org.nexo.messagingservice.repository.MessageRepository;
import org.nexo.messagingservice.service.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final UserGrpcClient userGrpcClient;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ConversationResponseDTO getOrCreateDirectConversation(String keycloakUserId, Long recipientUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        if (user.getUserId() == recipientUserId) {
            throw new ResourceNotFoundException("Cannot create conversation with yourself");
        }
        Long user1 = user.getUserId();
        if (userGrpcClient.getUserById(user1) == null) {
            throw new ResourceNotFoundException("Recipient user not found");
        }
        if (userGrpcClient.getUserById(recipientUserId) == null) {
            throw new ResourceNotFoundException("Recipient user not found");
        }
        Optional<ConversationModel> existing = conversationRepository.findDirectConversationBetweenUsers(user1,
                recipientUserId);
        if (existing.isPresent()) {
            log.info("Found existing conversation between {} and {}", user1, recipientUserId);
            return mapToDto(existing.get(), user1);
        }
        ConversationModel conversation = ConversationModel.builder().build();
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation, user1);
        addParticipant(conversation, recipientUserId);

        return mapToDto(conversation, user1);
    }

    public ConversationResponseDTO getConversationById(Long conversationId, Long requestingUserId) {
        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!isUserParticipant(conversationId, requestingUserId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        return mapToDto(conversation, requestingUserId);
    }

    public void archiveConversation(Long conversationId, String requestingUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(requestingUserId);
        Long userId = user.getUserId();
        ConversationParticipantModel participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        participant.setArchived(!participant.isArchived());
        participantRepository.save(participant);
    }

    public void muteConversation(Long conversationId, String requestingUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(requestingUserId);
        Long userId = user.getUserId();
        ConversationParticipantModel participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        participant.setMuted(!participant.isMuted());
        participantRepository.save(participant);

    }

    private boolean isUserParticipant(Long conversationId, Long userId) {
        return conversationRepository.isUserParticipant(conversationId, userId);
    }

    private void addParticipant(ConversationModel conversation, Long userId) {
        ConversationParticipantModel participant = new ConversationParticipantModel();
        participant.setConversation(conversation);
        participant.setUserId(userId);
        participantRepository.save(participant);
    }

    private ConversationResponseDTO mapToDto(ConversationModel conversation, Long requestingUserId) {
        List<Long> participantUserIds = participantRepository
                .findActiveUserIdsByConversationId(conversation.getId());

        List<UserDTOResponse2> participants = userGrpcClient.getUsersByIds(participantUserIds);

        List<UserDTO> participantDTOs = participants.stream()
                .map(u -> UserDTO.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .avatarUrl(u.getAvatar())
                        .fullName(u.getFullName())
                        .build())
                .toList();

        UserDTO otherUser = participantDTOs.stream()
                .filter(u -> u.getId() != requestingUserId)
                .findFirst()
                .orElse(null);

        String displayName = otherUser != null
                ? otherUser.getUsername()
                : "Unknown User";

        String displayAvatar = otherUser != null ? otherUser.getAvatarUrl() : null;

        MessageDTO lastMessage = null;
        if (conversation.getLastMessageId() != null) {
            Optional<MessageModel> lastMsg = messageRepository
                    .findById(conversation.getLastMessageId());
            if (lastMsg.isPresent()) {
                lastMessage = mapMessageToDto(lastMsg.get());
            }
        }
        Long unreadCount = getUnreadCount(conversation.getId(), requestingUserId);

        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .name(displayName)
                .avatarUrl(displayAvatar)
                .participants(participantDTOs)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    public PageModelResponse<ConversationResponseDTO> getUserConversations(String keycloakUserId, Pageable pageable) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();
        Page<ConversationModel> conversations = conversationRepository
                .findByUserIdOrderByLastMessageAtDesc(userId, pageable);
        PageModelResponse<ConversationResponseDTO> pageModelResponse = PageModelResponse
                .<ConversationResponseDTO>builder()
                .content(conversations.map(conv -> mapToDto(conv, userId)).getContent())
                .pageNo(conversations.getNumber())
                .pageSize(conversations.getSize())
                .totalElements(conversations.getTotalElements())
                .totalPages(conversations.getTotalPages())
                .build();
        return pageModelResponse;
    }

    private MessageDTO mapMessageToDto(MessageModel message) {
        UserDTOResponse sender = userGrpcClient.getUserById(message.getSenderUserId());
        UserDTO senderDto = UserDTO.builder()
                .id(sender.getId())
                .username(sender.getUsername())
                .avatarUrl(sender.getAvatar())
                .build();

        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sender(senderDto)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private Long getUnreadCount(Long conversationId, Long userId) {
        ConversationParticipantModel participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElse(null);

        if (participant == null || participant.getLastReadMessageId() == null) {
            return messageRepository.countUnreadMessages(conversationId, userId, 0L);
        }

        return messageRepository.countUnreadMessages(
                conversationId,
                userId,
                participant.getLastReadMessageId());
    }
}
