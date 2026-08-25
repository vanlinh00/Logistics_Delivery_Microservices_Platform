package com.logistics.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
@Slf4j
public class TotpService {

    private static final int SECRET_SIZE = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final String HMAC_ALGO = "HmacSHA1";

    public String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        new SecureRandom().nextBytes(buffer);
        Base32 base32 = new Base32();
        return base32.encodeToString(buffer).replace("=", "");
    }

    public String generateQrCodeUri(String secret, String accountName, String issuer) {
        String encodedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedIssuer, encodedAccount, secret, encodedIssuer, DIGITS, TIME_STEP_SECONDS);
    }

    public boolean verifyCode(String secret, String inputCode) {
        if (secret == null || inputCode == null || inputCode.length() != DIGITS) {
            return false;
        }

        try {
            long currentWindow = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
            // Check current window and +/- 1 window for clock drift tolerance
            for (int i = -1; i <= 1; i++) {
                String hash = generateTotpForWindow(secret, currentWindow + i);
                if (hash.equals(inputCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error verifying TOTP code: {}", e.getMessage());
            return false;
        }
    }

    private String generateTotpForWindow(String secret, long window) throws Exception {
        Base32 base32 = new Base32();
        byte[] key = base32.decode(secret);

        byte[] data = new byte[8];
        long value = window;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(key, HMAC_ALGO));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1_000_000;

        return String.format("%06d", truncatedHash);
    }
}
