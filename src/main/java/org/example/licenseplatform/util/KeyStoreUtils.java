package org.example.licenseplatform.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * 密钥库工具类：用于从 JKS 文件中加载私钥和公钥
 */
public class KeyStoreUtils {

    /**
     * 从 JKS 中加载私钥（用于服务端签名）
     *
     * @param keystorePath JKS 文件路径
     * @param alias 密钥别名
     * @param storePass 密钥库密码
     * @param keyPass 密钥条目密码
     * @return 私钥对象
     */
    public static PrivateKey loadPrivateKeyFromJKS(String keystorePath,
                                                   String alias,
                                                   String storePass,
                                                   String keyPass) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, storePass.toCharArray());
        }
        return (PrivateKey) keyStore.getKey(alias, keyPass.toCharArray());
    }

    /**
     * 从 JKS 中加载公钥（用于客户端验签）
     *
     * @param keystorePath JKS 文件路径
     * @param alias 密钥别名
     * @param storePass 密钥库密码
     * @return 公钥对象
     */
    public static PublicKey loadPublicKeyFromJKS(String keystorePath,
                                                 String alias,
                                                 String storePass) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, storePass.toCharArray());
        }
        Certificate cert = keyStore.getCertificate(alias);
        return cert.getPublicKey();
    }


    /**
     * 从 .cer 文件中加载公钥
     *
     * @param cerPath 证书路径（.cer 文件）
     * @return PublicKey 公钥对象
     */
    public static PublicKey loadPublicKeyFromCer(String cerPath) {
        try (InputStream in = new FileInputStream(cerPath)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(in);
            return certificate.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("加载公钥失败：" + cerPath, e);
        }
    }
}
