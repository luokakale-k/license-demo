package org.example.licenseplatform.util;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 签名工具类：用于生成和验证签名
 */
public class SignatureUtils {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    /**
     * 使用私钥对原始数据进行签名
     *
     * @param data 待签名内容（通常为 JSON 字符串）
     * @param privateKey 私钥
     * @return Base64 编码的签名字符串
     */
    public static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        return Base64.getEncoder().encodeToString(signed);
    }

    /**
     * 使用公钥验证签名是否合法
     *
     * @param data 原始数据
     * @param signatureBase64 Base64 编码的签名字符串
     * @param publicKey 公钥
     * @return true：验证通过；false：验证失败（被篡改或伪造）
     */
    public static boolean verify(String data, String signatureBase64, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(signatureBytes);
    }
}
