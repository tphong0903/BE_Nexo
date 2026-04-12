package org.nexo.userservice.controller;

import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.service.RecommendationDataExportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/recommendation")
@RequiredArgsConstructor
public class RecommendationInternalController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${recommendation.internal-token:}")
    private String internalTokenConfig;

    private final RecommendationDataExportService recommendationDataExportService;

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<?> exportUsers(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        if (!isValidInternalToken(internalToken)) {
            return unauthorizedResponse("Unauthorized internal request");
        }

        return ResponseData.builder()
                .status(HttpStatus.OK.value())
                .message("Export users for recommendation success")
                .data(recommendationDataExportService.exportUsers())
                .build();
    }

    @GetMapping(value = "/follows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<?> exportFollows(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        if (!isValidInternalToken(internalToken)) {
            return unauthorizedResponse("Unauthorized internal request");
        }

        return ResponseData.builder()
                .status(HttpStatus.OK.value())
                .message("Export follows for recommendation success")
                .data(recommendationDataExportService.exportFollows())
                .build();

    }

    @GetMapping(value = "/blocks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<?> exportBlocks(
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken) {
        if (!isValidInternalToken(internalToken)) {
            return unauthorizedResponse("Unauthorized internal request");
        }

        return ResponseData.builder()
                .status(HttpStatus.OK.value())
                .message("Export blocks for recommendation success")
                .data(recommendationDataExportService.exportBlocks())
                .build();
    }

    private boolean isValidInternalToken(String internalToken) {
        return internalTokenConfig != null
                && !internalTokenConfig.isBlank()
                && internalTokenConfig.equals(internalToken);
    }

    private ResponseData<?> unauthorizedResponse(String message) {
        return ResponseData.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message(message)
                .data(null)
                .build();
    }
}
