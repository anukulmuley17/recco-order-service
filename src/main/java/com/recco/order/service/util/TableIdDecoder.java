package com.recco.order.service.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class TableIdDecoder {

    private static final String SECRET_KEY = "your-very-strong-secret-key";
    private static final String HMAC_ALGO = "HmacSHA256";

    // Decode and verify signature
    public String decodeTableId(String tableToken) {
        try {
            String[] parts = tableToken.split("\\.");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String encodedTableId = parts[0];
            String providedSignature = parts[1];

            String tableId = new String(Base64.getUrlDecoder().decode(encodedTableId), StandardCharsets.UTF_8);

            // Verify HMAC
            String expectedSignature = generateHmac(tableId);
            if (!providedSignature.equals(expectedSignature)) {
                throw new IllegalArgumentException("Invalid table token signature");
            }

            return tableId;
        } catch (Exception e) {
            throw new RuntimeException("Error decoding tableId", e);
        }
    }

    private String generateHmac(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }
}
