package org.nexo.messagingservice.service.Impl;

import java.util.List;
import java.util.Optional;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse;
import org.nexo.grpc.user.UserServiceProto.UserDTOResponse2;
import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.MessageDTO;
import org.nexo.messagingservice.dto.NicknameRequest;
import org.nexo.messagingservice.dto.PageModelResponse;
import org.nexo.messagingservice.dto.UserDTO;
import org.nexo.messagingservice.enums.EConversationStatus;
import org.nexo.messagingservice.exception.ResourceNotFoundException;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.model.ConversationModel;
import org.nexo.messagingservice.model.ConversationParticipantModel;
import org.nexo.messagingservice.model.MessageModel;
import org.nexo.messagingservice.repository.ConversationParticipantRepository;
import org.nexo.messagingservice.repository.ConversationRepository;
import org.nexo.messagingservice.repository.MessageRepository;
import org.nexo.messagingservice.service.MessageService;
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
    private final MessageService messageService;

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

        boolean isBlocked = userGrpcClient.isUserBlocked(user1, recipientUserId);
        if (isBlocked) {
            throw new SecurityException("Cannot create conversation with blocked user");
        }

        Optional<ConversationModel> existing = conversationRepository.findDirectConversationBetweenUsers(user1,
                recipientUserId);
        if (existing.isPresent()) {
            ConversationModel conv = existing.get();
            if (conv.getStatus() == EConversationStatus.BLOCKED) {
                throw new SecurityException("Conversation is blocked");
            }
            log.info("Found existing conversation between {} and {}", user1, recipientUserId);
            return mapToDto(conv, user1);
        }

        boolean areMutualFriends = userGrpcClient.areMutualFriends(user1, recipientUserId);
        EConversationStatus initialStatus = areMutualFriends ? EConversationStatus.NORMAL : EConversationStatus.PENDING;

        ConversationModel conversation = ConversationModel.builder()
                .status(initialStatus)
                .build();
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation, user1);
        addParticipant(conversation, recipientUserId);

        log.info("Created {} conversation between {} and {}", initialStatus, user1, recipientUserId);
        return mapToDto(conversation, user1);
    }

    @Transactional
    public void acceptConversationRequest(Long conversationId, String keycloakUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        if (conversation.getStatus() != EConversationStatus.PENDING) {
            throw new IllegalStateException("Conversation is not in pending status");
        }

        conversation.setStatus(EConversationStatus.NORMAL);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void declineConversationRequest(Long conversationId, String keycloakUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        if (conversation.getStatus() != EConversationStatus.PENDING) {
            throw new IllegalStateException("Conversation is not in pending status");
        }

        conversation.setStatus(EConversationStatus.DECLINED);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void handleBlockStatusChange(Long userId1, Long userId2, boolean isBlocked) {
        Optional<ConversationModel> conversation = conversationRepository
                .findDirectConversationBetweenUsers(userId1, userId2);

        if (conversation.isPresent()) {
            ConversationModel conv = conversation.get();
            if (isBlocked) {
                conv.setStatus(EConversationStatus.BLOCKED);
                conv.setBlockedByUserId(userId1);
            } else {
                conv.setStatus(EConversationStatus.NORMAL);
                conv.setBlockedByUserId(null);
            }
            conversationRepository.save(conv);
            log.info("Updated conversation {} block status: blocked={}", conv.getId(), isBlocked);
        }
    }

    public ConversationResponseDTO getConversationById(Long conversationId, Long requestingUserId) {
        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!isUserParticipant(conversationId, requestingUserId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        if (conversation.getStatus() == EConversationStatus.BLOCKED
                && conversation.getBlockedByUserId() != null
                && conversation.getBlockedByUserId().equals(requestingUserId)) {
            throw new ResourceNotFoundException("Conversation not found");
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
                        .nickname(
                                participantRepository.findByConversationIdAndUserId(conversation.getId(), u.getId())
                                        .map(ConversationParticipantModel::getNickname)
                                        .orElse(null))
                        .onlineStatus(u.getOnlineStatus())
                        .build())
                .toList();

        UserDTO otherUser = participantDTOs.stream()
                .filter(u -> u.getId() != requestingUserId)
                .findFirst()
                .orElse(null);

        String displayName = "Null";
        if (otherUser != null) {
            ConversationParticipantModel participant = participantRepository
                    .findByConversationIdAndUserId(conversation.getId(), requestingUserId)
                    .orElse(null);
            if (participant != null && participant.getNickname() != null && !participant.getNickname().isEmpty()) {
                displayName = participant.getNickname();
            } else {
                displayName = otherUser.getFullName();
            }
        }

        String displayAvatar = otherUser != null ? otherUser.getAvatarUrl() : null;
        Boolean displayOnlineStatus = otherUser != null ? otherUser.getOnlineStatus() : null;

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
                .senderUserId(requestingUserId)
                .fullname(displayName)
                .avatarUrl(displayAvatar)
                .onlineStatus(displayOnlineStatus)
                .participants(participantDTOs)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .lastMessageAt(conversation.getLastMessageAt())
                .createdAt(conversation.getCreatedAt())
                .status(conversation.getStatus())
                .isBlockedByMe(conversation.getBlockedByUserId() != null
                        && conversation.getBlockedByUserId().equals(requestingUserId))
                .lastReadMessageId(messageService.getLastReadMessageId(conversation.getId(), otherUser.getId()))
                .build();
    }

    public PageModelResponse<ConversationResponseDTO> getUserConversations(String keycloakUserId, Pageable pageable) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();
        Page<ConversationModel> conversations = conversationRepository
                .findNormalConversationsByUserId(userId, pageable);

        List<ConversationResponseDTO> allConversations = conversations.stream()
                .map(conv -> mapToDto(conv, userId))
                .toList();

        PageModelResponse<ConversationResponseDTO> pageModelResponse = PageModelResponse
                .<ConversationResponseDTO>builder()
                .content(allConversations)
                .pageNo(conversations.getNumber())
                .pageSize(conversations.getSize())
                .totalElements((long) allConversations.size())
                .totalPages(conversations.getTotalPages())
                .build();
        return pageModelResponse;
    }

    public PageModelResponse<ConversationResponseDTO> getPendingRequests(String keycloakUserId, Pageable pageable) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        Page<ConversationModel> pendingConversations = conversationRepository
                .findPendingConversationsByUserId(userId, pageable);

        PageModelResponse<ConversationResponseDTO> pageModelResponse = PageModelResponse
                .<ConversationResponseDTO>builder()
                .content(pendingConversations.map(conv -> mapToDto(conv, userId)).getContent())
                .pageNo(pendingConversations.getNumber())
                .pageSize(pendingConversations.getSize())
                .totalElements(pendingConversations.getTotalElements())
                .totalPages(pendingConversations.getTotalPages())
                .build();
        return pageModelResponse;
    }

    public PageModelResponse<ConversationResponseDTO> getUnreadConversations(String keycloakUserId, Pageable pageable) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        Page<ConversationModel> normalConversations = conversationRepository
                .findNormalConversationsByUserId(userId, pageable);

        List<ConversationResponseDTO> unreadConversations = normalConversations.stream()
                .filter(conv -> !(conv.getStatus() == EConversationStatus.BLOCKED
                        && conv.getBlockedByUserId() != null
                        && conv.getBlockedByUserId().equals(userId)))
                .map(conv -> mapToDto(conv, userId))
                .filter(dto -> dto.getUnreadCount() != null && dto.getUnreadCount() > 0)
                .toList();

        PageModelResponse<ConversationResponseDTO> pageModelResponse = PageModelResponse
                .<ConversationResponseDTO>builder()
                .content(unreadConversations)
                .pageNo(normalConversations.getNumber())
                .pageSize(normalConversations.getSize())
                .totalElements((long) unreadConversations.size())
                .totalPages(normalConversations.getTotalPages())
                .build();
        return pageModelResponse;
    }

    public PageModelResponse<ConversationResponseDTO> getArchivedConversations(String keycloakUserId,
            Pageable pageable) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        Page<ConversationModel> archivedConversations = conversationRepository
                .findArchivedConversationsByUserId(userId, pageable);

        List<ConversationResponseDTO> filteredConversations = archivedConversations.stream()
                .filter(conv -> !(conv.getStatus() == EConversationStatus.BLOCKED
                        && conv.getBlockedByUserId() != null
                        && conv.getBlockedByUserId().equals(userId)))
                .map(conv -> mapToDto(conv, userId))
                .toList();

        PageModelResponse<ConversationResponseDTO> pageModelResponse = PageModelResponse
                .<ConversationResponseDTO>builder()
                .content(filteredConversations)
                .pageNo(archivedConversations.getNumber())
                .pageSize(archivedConversations.getSize())
                .totalElements((long) filteredConversations.size())
                .totalPages(archivedConversations.getTotalPages())
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

    public void acceptMessageRequest(Long conversationId, String keycloakUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        if (conversation.getStatus() != EConversationStatus.PENDING) {
            throw new IllegalStateException("Conversation is not pending");
        }

        conversation.setStatus(EConversationStatus.NORMAL);
        conversationRepository.save(conversation);

    }

    public void declineMessageRequest(Long conversationId, String keycloakUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        if (conversation.getStatus() != EConversationStatus.PENDING) {
            throw new IllegalStateException("Conversation is not pending");
        }

        conversation.setStatus(EConversationStatus.DECLINED);
        conversationRepository.save(conversation);

    }

    public ConversationResponseDTO setNickname(Long conversationId, String keycloakUserId, NicknameRequest request) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();

        if (!isUserParticipant(conversationId, userId)) {
            throw new SecurityException("User is not a participant in this conversation");
        }

        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        Long targetUserId = request.getUserId();
        ConversationParticipantModel participant = participantRepository
                .findByConversationIdAndUserId(conversationId, targetUserId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Target user is not a participant in this conversation"));

        participant.setNickname(request.getNickname());
        participantRepository.save(participant);

        return mapToDto(conversation, userId);
    }

    public ConversationResponseDTO getNickname(Long conversationId, String keycloakUserId) {
        UserServiceProto.UserDto user = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = user.getUserId();
        ConversationModel conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        return mapToDto(conversation, userId);
    }
}