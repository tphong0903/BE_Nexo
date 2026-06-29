package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.response.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final WebClient geminiWebClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    public boolean isImageViolated(String imageUrl) {

        try {

            String prompt = """
                    You are an image moderation classifier.

                    Return only:
                    true
                    or
                    false

                    Return true if the image contains:
                    - nudity
                    - pornography
                    - explicit sexual content
                    - graphic violence
                    - gore
                    - self-harm
                    - hate symbols
                    - threatening weapons

                    Otherwise return false.
                    """;

            Map<String, Object> request = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt),
                                            Map.of(
                                                    "fileData",
                                                    Map.of(
                                                            "mimeType", "image/jpeg",
                                                            "fileUri", imageUrl
                                                    )
                                            )
                                    )
                            )
                    )
            );

            GeminiResponse response = geminiWebClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/"
                            + model
                            + ":generateContent?key="
                            + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            if (response == null
                    || response.getCandidates() == null
                    || response.getCandidates().isEmpty()) {

                return false;
            }

            String result = response.getCandidates()
                    .getFirst()
                    .getContent()
                    .getParts()
                    .getFirst()
                    .getText();

            log.info("Gemini response: {}", result);

            return result.trim().equalsIgnoreCase("true");

        } catch (Exception ex) {

            log.error("Gemini failed", ex);
            return false;
        }

    }

}