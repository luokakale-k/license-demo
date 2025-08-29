package org.example.licenseplatform.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HMAC 签名工具类（适用于服务端和客户端）
 */
public class HmacUtils {

    private static final String HMAC_ALGO = "HmacSHA256";

    /**
     * 生成 HMAC 签名
     *
     * @param data      原始数据
     * @param secretKey 秘钥
     * @return Base64 编码后的签名
     */
    public static String sign(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC 签名失败", e);
        }
    }

    /**
     * 校验签名
     */
    public static boolean verify(String data, String signature, String secretKey) {
        return sign(data, secretKey).equals(signature);
    }
}
