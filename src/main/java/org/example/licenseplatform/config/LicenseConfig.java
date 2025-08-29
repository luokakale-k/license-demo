package org.example.licenseplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * License 配置项读取类
 * 绑定 application.yml 中以 license 开头的配置项
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "license")
public class LicenseConfig {

    /**
     * 私钥配置（用于生成 license 签名）
     * 对应 application.yml 中 license.private-key
     */
    private PrivateKeyConfig privateKey;

    /**
     * 公钥配置（用于客户端校验 license）
     * 对应 application.yml 中 license.public-key
     */
    private PublicKeyConfig publicKey;

    /**
     * License 文件输出路径（.lic 文件生成目录）
     * 示例：/Users/kaka/licenses/
     */
    private String outputPath;

    /**
     * 时间回拨检测的 HMAC 加密密钥
     * 用于校验启动时间记录文件是否被篡改
     */
    private String timeSecret;

    /**
     * 客户端配置项（License 校验时使用）
     * 对应 application.yml 中 license.client
     */
    private ClientConfig client;

    /**
     * 内部类：私钥相关配置
     */
    @Data
    public static class PrivateKeyConfig {
        /** keystore 文件绝对路径（.jks 格式） */
        private String keystorePath;

        /** keystore 中的别名 */
        private String alias;

        /** keystore 密码 */
        private String storePass;

        /** 私钥条目的访问密码 */
        private String keyPass;
    }

    /**
     * 内部类：公钥相关配置
     */
    @Data
    public static class PublicKeyConfig {
        /** 公钥证书路径（.cer 格式） */
        private String cerPath;
    }

    /**
     * 内部类：客户端运行时加载 License 所需路径
     */
    @Data
    public static class ClientConfig {
        /** License 文件路径（.lic） */
        private String licensePath;

        /** 公钥证书路径（建议使用 ${license.public-key.cer-path} 引用） */
        private String publicKeyPath;

        /** 上次启动时间记录文件（用于时间回拨防护） */
        private String timeRecordPath;
    }
}
