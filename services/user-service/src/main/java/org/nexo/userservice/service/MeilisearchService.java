package org.nexo.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.Searchable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.nexo.userservice.dto.UserResonspeAdmin;
import org.nexo.userservice.dto.UserSearchDocument;
import org.nexo.userservice.dto.UserSearchResponse;
import org.nexo.userservice.dto.UserSearchResponseAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeilisearchService {

        private final Client meilisearchClient;
        private final ObjectMapper objectMapper;

        @Value("${meilisearch.index.users}")
        private String usersIndexName;

        private Index usersIndex;

        private Index usersAdminIndex;

        @PostConstruct
        public void init() {
                try {
                        usersIndex = meilisearchClient.index(usersIndexName);
                        usersAdminIndex = meilisearchClient.index(usersIndexName + "_admin");

                        usersIndex.updateSearchableAttributesSettings(new String[] {
                                        "username", "fullName"
                        });
                        usersAdminIndex.updateSearchableAttributesSettings(new String[] {
                                        "username", "fullName", "email"
                        });

                        usersIndex.updateSortableAttributesSettings(new String[] {
                                        "username", "fullName"
                        });
                        usersAdminIndex.updateSortableAttributesSettings(new String[] {
                                        "username", "fullName", "email", "account_status", "violation_count"
                        });

                        usersAdminIndex.updateFilterableAttributesSettings(new String[] {
                                        "account_status"
                        });

                        log.info("Meilisearch index '{}' initialized successfully", usersIndexName);
                } catch (Exception e) {
                        log.error("Failed to initialize Meilisearch index", e);
                }
        }

        public void indexUser(UserSearchDocument document) throws JsonProcessingException, MeilisearchException {
                String json = objectMapper.writeValueAsString(document);
                usersIndex.addDocuments(json);
                log.info("Indexed user: {}", document.getId());
        }

        public void updateUser(UserSearchDocument document, UserResonspeAdmin documentAdmin)
                        throws JsonProcessingException, MeilisearchException {
                String json = objectMapper.writeValueAsString(document);
                usersIndex.updateDocuments(json);
                String jsonAdmin = objectMapper.writeValueAsString(documentAdmin);
                usersAdminIndex.updateDocuments(jsonAdmin);
                log.info("Updated user in index: {}", document.getId());
        }

        public void deleteUser(Long userId) throws MeilisearchException {
                usersIndex.deleteDocument(String.valueOf(userId));
                log.info("Deleted user from index: {}", userId);
        }

        public UserSearchResponse searchUsers(String query, Integer limit, Integer offset, String filter)
                        throws MeilisearchException {
                int searchLimit = limit != null ? limit : 10;
                int searchOffset = offset != null ? offset : 0;

                SearchRequest searchRequest = SearchRequest.builder()
                                .q(query)
                                .limit(searchLimit)
                                .offset(searchOffset)
                                .filter(filter != null ? new String[] { filter } : null)
                                .build();

                Searchable result = usersIndex.search(searchRequest);

                List<UserSearchDocument> users = result.getHits().stream()
                                .map(hit -> objectMapper.convertValue(hit, UserSearchDocument.class))
                                .collect(Collectors.toList());

                return UserSearchResponse.builder()
                                .users(users)
                                .totalHits(users.size())
                                .limit(searchLimit)
                                .offset(searchOffset)
                                .processingTimeMs((long) result.getProcessingTimeMs())
                                .query(query)
                                .build();
        }

        public UserSearchResponseAdmin searchUsersAdmin(String query, Integer limit, Integer offset, String filter)
                        throws MeilisearchException {
                int searchLimit = limit != null ? limit : 10;
                int searchOffset = offset != null ? offset : 0;

                SearchRequest searchRequest = SearchRequest.builder()
                                .q(query)
                                .limit(searchLimit)
                                .offset(searchOffset)
                                .filter(filter != null ? new String[] { filter } : null)
                                .build();

                Searchable result = usersAdminIndex.search(searchRequest);

                List<UserResonspeAdmin> users = result.getHits().stream()
                                .map(hit -> objectMapper.convertValue(hit, UserResonspeAdmin.class))
                                .collect(Collectors.toList());

                return UserSearchResponseAdmin.builder()
                                .users(users)
                                .totalHits(users.size())
                                .limit(searchLimit)
                                .offset(searchOffset)
                                .processingTimeMs((long) result.getProcessingTimeMs())
                                .query(query)
                                .build();
        }

        public void reindexAllUsers(List<UserSearchDocument> users, List<UserResonspeAdmin> documentsAdmin)
                        throws JsonProcessingException, MeilisearchException {
                String json = objectMapper.writeValueAsString(users);
                usersIndex.addDocuments(json);
                String jsonAdmin = objectMapper.writeValueAsString(documentsAdmin);
                usersAdminIndex.addDocuments(jsonAdmin);
                log.info("Reindexed {} users", users.size());
        }
}
