package org.example.licenseplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.licenseplatform.config.LicenseConfig;
import org.example.licenseplatform.model.LicenseContent;
import org.example.licenseplatform.model.LicenseRequest;
import org.example.licenseplatform.util.JsonUtils;
import org.example.licenseplatform.util.KeyStoreUtils;
import org.example.licenseplatform.util.SignatureUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;

/**
 * License 服务类：用于根据前端请求生成签名后的 License 文件
 */
@Service
public class LicenseService {

    private final ObjectMapper objectMapper;
    private final LicenseConfig licenseConfig;

    @Autowired
    public LicenseService(LicenseConfig licenseConfig) {
        this.objectMapper = JsonUtils.getMapper(); // 使用统一的 JSON 工具配置
        this.licenseConfig = licenseConfig;
    }

    /**
     * 根据 LicenseRequest 请求生成签名后的 License 文件
     *
     * @param request 前端提交的 License 请求参数
     * @return 是否生成成功
     */
    public boolean generateLicense(LicenseRequest request) {
        try {
            // 1. 构建 License 内容（签名前）
            LicenseContent content = new LicenseContent();
            content.setLicenseId(request.getLicenseId());
            content.setCustomer(request.getCustomer());
            content.setEdition(request.getEdition());
            content.setIssueDate(request.getIssueDate());
            content.setExpireDate(request.getExpireDate());
            content.setFeatures(request.getFeatures());

            // 2. 设置绑定机器列表（支持集群部署）
            content.setBoundMachines(request.getBoundMachines());

            // 3. 设置部署模式：standalone / cluster
            content.setMode(request.getMode());

            // 4. 初始签名字段设为空（参与签名的数据中不能包含签名本身）
            content.setSignature(null);

            // 首次使用时间为空
            content.setFirstUsedAt(null);

            // 5. 将 License 内容转为 JSON 字符串（用于签名）
            String jsonToSign = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(content);

            // 6. 加载本地 JKS 私钥
            PrivateKey privateKey = KeyStoreUtils.loadPrivateKeyFromJKS(
                    licenseConfig.getPrivateKey().getKeystorePath(),
                    licenseConfig.getPrivateKey().getAlias(),
                    licenseConfig.getPrivateKey().getStorePass(),
                    licenseConfig.getPrivateKey().getKeyPass()
            );

            // 7. 使用私钥进行签名
            String signature = SignatureUtils.sign(jsonToSign, privateKey);
            content.setSignature(signature);

            // 8. 构造 License 文件输出路径
            String outputPath = licenseConfig.getOutputPath() + request.getLicenseId() + ".lic";
            File outputFile = new File(outputPath);
            Files.createDirectories(Paths.get(outputFile.getParent())); // 确保目录存在

            // 9. 将最终带签名的 JSON 内容写入 .lic 文件
            String finalJson = objectMapper.writeValueAsString(content);
            Files.write(outputFile.toPath(), finalJson.getBytes(StandardCharsets.UTF_8));

            return true;
        } catch (Exception e) {
            e.printStackTrace(); // 实际使用中应替换为日志记录
            return false;
        }
    }
}
