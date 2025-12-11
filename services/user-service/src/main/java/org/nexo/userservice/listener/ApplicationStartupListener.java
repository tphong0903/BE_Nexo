package org.nexo.userservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.nexo.userservice.dto.UserResponseAdmin;
import org.nexo.userservice.dto.UserSearchDocument;
import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.MeilisearchService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupListener {

    private final MeilisearchService meilisearchService;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application is ready. Starting initial user indexing...");
        try {
            List<UserModel> allUsers = userRepository.findAllByAccountStatus(EAccountStatus.ACTIVE);
            List<UserModel> allUsersAdmin = userRepository.findAll();

            if (allUsers.isEmpty() && allUsersAdmin.isEmpty()) {
                log.info("No users found in database. Skipping indexing.");
                return;
            }
            List<UserSearchDocument> documents = allUsers.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());
            List<UserResponseAdmin> documentsAdmin = allUsersAdmin.stream()
                    .map(this::convertToDocumentAdmin)
                    .collect(Collectors.toList());
            meilisearchService.reindexAllUsers(documents, documentsAdmin);
            log.info("Successfully indexed {} users to Meilisearch on startup", documents.size());
        } catch (Exception e) {
            log.error("Failed to index users on startup. Search may not work properly.", e);
        }
    }

    private UserSearchDocument convertToDocument(UserModel user) {
        return UserSearchDocument.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .build();
    }

    private UserResponseAdmin convertToDocumentAdmin(UserModel user) {
        return UserResponseAdmin.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .violationCount(user.getViolationCount())
                .build();
    }
}
