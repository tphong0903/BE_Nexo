package org.nexo.authservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@Slf4j
public class JwtUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isEmailVerified(String accessToken) {
        try {
            // Split JWT token to get payload
            String[] tokenParts = accessToken.split("\\.");
            if (tokenParts.length != 3) {
                log.warn("Invalid JWT token format");
                return false;
            }

            // Decode base64 payload
            String payload = tokenParts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            // Parse JSON payload
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);

            // Check email_verified claim
            JsonNode emailVerifiedNode = payloadNode.get("email_verified");
            if (emailVerifiedNode != null) {
                boolean emailVerified = emailVerifiedNode.asBoolean();
                log.info("Email verified status from token: {}", emailVerified);
                return emailVerified;
            }

            log.warn("email_verified claim not found in token");
            return false;

        } catch (Exception e) {
            log.error("Error checking email verification status: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String accessToken) {
        try {
            String[] tokenParts = accessToken.split("\\.");
            if (tokenParts.length != 3) {
                return null;
            }

            String payload = tokenParts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            JsonNode subNode = payloadNode.get("sub");

            return subNode != null ? subNode.asText() : null;

        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }

    public String getEmailFromToken(String accessToken) {
        try {
            String[] tokenParts = accessToken.split("\\.");
            if (tokenParts.length != 3) {
                return null;
            }

            String payload = tokenParts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            JsonNode emailNode = payloadNode.get("email");

            return emailNode != null ? emailNode.asText() : null;

        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    public String getUsernameFromToken(String accessToken) {
        try {
            String[] tokenParts = accessToken.split("\\.");
            if (tokenParts.length != 3) {
                return null;
            }

            String payload = tokenParts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            JsonNode usernameNode = payloadNode.get("preferred_username");

            return usernameNode != null ? usernameNode.asText() : null;

        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    public String getFullnameFromToken(String accessToken) {
        try {
            String[] tokenParts = accessToken.split("\\.");
            if (tokenParts.length != 3) {
                return null;
            }

            String payload = tokenParts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            JsonNode fullnameNode = payloadNode.get("name");

            return fullnameNode != null ? fullnameNode.asText() : null;

        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }
}
