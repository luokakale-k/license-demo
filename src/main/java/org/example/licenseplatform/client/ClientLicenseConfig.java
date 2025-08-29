package org.example.licenseplatform.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Data
@Component
@ConfigurationProperties(prefix = "license.client")
public class ClientLicenseConfig {

    /** License 文件路径（JSON 格式） */
    private String licensePath;

    /** 公钥证书路径（用于验签） */
    private String publicKeyPath;

    /** 记录最后启动时间的文件路径（防时间回拨） */
    private String timeRecordPath;

    /** HMAC 用于时间防篡改签名的密钥（从 license.time-secret 注入） */
    @Value("${license.time-secret}")
    private String timeSecret;

    /**
     * 加载公钥（从 X.509 证书文件中）
     */
    public PublicKey loadPublicKey() {
        try (FileInputStream fis = new FileInputStream(publicKeyPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
            return cert.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("加载公钥失败：" + e.getMessage(), e);
        }
    }
}
