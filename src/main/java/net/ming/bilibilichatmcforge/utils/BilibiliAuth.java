package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import net.ming.bilibilichatmcforge.Config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class BilibiliAuth {
    private static final Gson GSON = new Gson();

    public static Map<String, String> getHeaders(String body) {
        String accessKey = Config.accessKey;
        String accessSecret = Config.accessSecret;

        if (accessKey == null || accessKey.isEmpty() || accessSecret == null || accessSecret.isEmpty()) {
            return new HashMap<>();
        }

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString();
        String contentMd5 = md5(body);

        Map<String, String> headerMap = new TreeMap<>();
        headerMap.put("x-bili-accesskeyid", accessKey);
        headerMap.put("x-bili-content-md5", contentMd5);
        headerMap.put("x-bili-signature-method", "HMAC-SHA256");
        headerMap.put("x-bili-signature-nonce", nonce);
        headerMap.put("x-bili-signature-version", "1.0");
        headerMap.put("x-bili-timestamp", timestamp);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }

        String signature = hmacSha256(accessSecret, sb.toString());
        headerMap.put("Authorization", signature);
        headerMap.put("Content-Type", "application/json");
        headerMap.put("Accept", "application/json");

        return headerMap;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hmacSha256(String secret, String message) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HMAC-SHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HMAC-SHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
