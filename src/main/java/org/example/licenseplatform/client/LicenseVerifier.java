package org.example.licenseplatform.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.licenseplatform.model.LicenseContent;
import org.example.licenseplatform.util.JsonUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

/**
 * License 校验器：负责整体加载和校验流程
 */
public class LicenseVerifier {

    private final ObjectMapper objectMapper = JsonUtils.getMapper();
    private final ClientLicenseConfig config;

    public LicenseVerifier(ClientLicenseConfig config) {
        this.config = config;
    }

    /**
     * 加载并验证本地 License 文件
     *
     * @return 校验通过的 LicenseContent 内容（可注入 LicenseContext）
     */
    public LicenseContent verify() {
        try {
            // 1. 读取 License 文件内容
            File licenseFile = new File(config.getLicensePath());
            if (!licenseFile.exists()) {
                throw new LicenseLoadException("未找到 License 文件：" + config.getLicensePath());
            }

            String json = new String(
                    Files.readAllBytes(Paths.get(config.getLicensePath())),
                    StandardCharsets.UTF_8
            );

            // 2. 反序列化为 LicenseContent 对象
            LicenseContent license = objectMapper.readValue(json, LicenseContent.class);

            // 3. 加载公钥
            PublicKey publicKey = config.loadPublicKey();

            // 4. 执行完整校验流程（签名、时间、硬件、时间回拨）
            LicenseValidator.validateSignature(license, publicKey);
            LicenseValidator.validateDate(license);
            LicenseValidator.validateHardware(license);
            LicenseValidator.validateTimeRollback(config.getTimeRecordPath(), config.getTimeSecret());

            // 5. 校验成功，返回 License 内容用于注入 LicenseContext
            return license;

        } catch (Exception e) {
            throw new LicenseLoadException("License 校验失败：" + e.getMessage(), e);
        }
    }
}
