package org.nexo.apigateway.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class JwtUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isEmailVerified(String accessToken) {
        try {
            // Split JWT token to get payload
            String[] tokenParts = accessToken.split("\\.");
            if (tokenParts.length != 3) {
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
                return emailVerified;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    //id cua keycloak
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
            return null;
        }
    }
}
