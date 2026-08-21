package net.ming.bilibilichatmcforge.utils;

import net.ming.bilibilichatmcforge.JsonConfigManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 按开放平台规范计算请求签名：六个 x-bili 头按 key 排序拼接后做 HMAC-SHA256。
 */
public class BilibiliAuth {

    public static Map<String, String> getHeaders(String body) {
        String accessKey = JsonConfigManager.getInstance().accessKey;
        String accessSecret = JsonConfigManager.getInstance().accessSecret;
        if (accessKey == null || accessKey.isEmpty() || accessSecret == null || accessSecret.isEmpty()) {
            return new TreeMap<>();
        }

        Map<String, String> headers = new TreeMap<>();
        headers.put("x-bili-accesskeyid", accessKey);
        headers.put("x-bili-content-md5", md5(body));
        headers.put("x-bili-signature-method", "HMAC-SHA256");
        headers.put("x-bili-signature-nonce", UUID.randomUUID().toString());
        headers.put("x-bili-signature-version", "1.0");
        headers.put("x-bili-timestamp", String.valueOf(System.currentTimeMillis() / 1000));

        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (canonical.length() > 0) canonical.append('\n');
            canonical.append(entry.getKey()).append(':').append(entry.getValue());
        }

        headers.put("Authorization", hmacSha256(accessSecret, canonical.toString()));
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    private static String md5(String input) {
        return digest("MD5", input);
    }

    private static String hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String digest(String algorithm, String input) {
        try {
            return toHex(MessageDigest.getInstance(algorithm).digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }
}
