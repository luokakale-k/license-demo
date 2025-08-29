package org.example.licenseplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.licenseplatform.client.LicenseLoadException;
import org.example.licenseplatform.common.Result;
import org.example.licenseplatform.config.LicenseConfig;
import org.example.licenseplatform.model.LicenseContent;
import org.example.licenseplatform.model.MachineInfo;
import org.example.licenseplatform.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Service
public class LicenseVerifierService {

    private final ObjectMapper objectMapper = JsonUtils.getMapper();

    @Autowired
    private LicenseConfig licenseConfig;

    public Result<?> verify(String licensePath, String publicKeyPath, String timeRecordPath) {
        try {
            log.info("校验 License 文件: {}", licensePath);

            // 1. 加载 License 文件并反序列化
            LicenseContent license = loadLicense(licensePath);

            // 2. 验签
            Result<?> signatureResult = verifySignature(license, publicKeyPath);
            if (!signatureResult.isSuccess()) return signatureResult;

            // 3. 校验生效时间 & 过期时间
            Result<?> timeResult = verifyTime(license);
            if (!timeResult.isSuccess()) return timeResult;

            // 4. 校验硬件指纹
            Result<?> machineResult = verifyMachineInfo(license);
            if (!machineResult.isSuccess()) return machineResult;

            // 5. 检查系统时间是否回拨
            Result<?> rollbackResult = verifyClockRollback(timeRecordPath);
            if (!rollbackResult.isSuccess()) return rollbackResult;

            // 如果首次使用时间为 null，则记录
            if (license.getFirstUsedAt() == null) {
                license.setFirstUsedAt(System.currentTimeMillis());

                // 重新签名 + 持久化到 .lic 文件
                updateLicenseFirstUsedAt(licensePath, license);
            }

            return Result.ok("License 校验通过");

        } catch (LicenseLoadException e) {
            log.error("License 加载异常", e);
            return Result.fail(5001, "License 加载失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("License 校验异常", e);
            return Result.fail(5002, "License 校验失败: " + e.getMessage());
        }
    }

    // 读取并反序列化 License
    private LicenseContent loadLicense(String licensePath) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(licensePath));
        String json = new String(bytes, StandardCharsets.UTF_8);
        return objectMapper.readValue(json, LicenseContent.class);
    }

    // 验证 License 签名
    private Result<?> verifySignature(LicenseContent license, String publicKeyPath) throws Exception {
        String signature = license.getSignature();
        if (signature == null || signature.isEmpty()) {
            return Result.fail(4001, "签名字段为空，非法 License");
        }

        license.setSignature(null);
        String unsignedJson = objectMapper.writeValueAsString(license);

        PublicKey publicKey = KeyStoreUtils.loadPublicKeyFromCer(publicKeyPath);
        boolean valid = SignatureUtils.verify(unsignedJson, signature, publicKey);
        if (!valid) {
            return Result.fail(4002, "签名验证失败，License 文件可能被篡改");
        }
        return Result.ok("签名验证通过");
    }

    // 验证生效时间与过期时间
    private Result<?> verifyTime(LicenseContent license) {
        long nowMillis = System.currentTimeMillis();
        long issueMillis = license.getIssueDate();
        long expireMillis = license.getExpireDate();

        if (nowMillis < issueMillis) {
            return Result.fail(4003, "License 尚未生效");
        }
        if (nowMillis > expireMillis) {
            return Result.fail(4004, "License 已过期");
        }
        return Result.ok("时间验证通过");
    }


    // 校验当前机器是否在授权机器列表中
    private Result<?> verifyMachineInfo(LicenseContent license) {
        if (license.getBoundMachines() == null || license.getBoundMachines().isEmpty()) {
            return Result.fail(4005, "License 中未配置绑定机器信息");
        }

        // 获取当前机器的硬件指纹
        String currentMac = MachineInfoUtils.getFirstMacAddress();
        String currentCpu = MachineInfoUtils.getCPUSerial();
        String currentBoard = MachineInfoUtils.getMainBoardSerial();

        String mode = license.getMode();

        // 单机模式：只比对第一台机器
        if ("standalone".equalsIgnoreCase(mode)) {
            MachineInfo only = license.getBoundMachines().get(0);
            boolean match =
                    safeEquals(only.getMacAddress(), currentMac) &&
                            safeEquals(only.getCpuSerial(), currentCpu) &&
                            safeEquals(only.getMainBoardSerial(), currentBoard);

            if (!match) {
                return Result.fail(4005, "硬件指纹不一致，当前机器非授权机器（standalone 模式）");
            }

            return Result.ok("机器指纹验证通过（standalone 模式）");
        }

        // 默认模式：cluster，遍历任意一台匹配即可
        boolean match = license.getBoundMachines().stream().anyMatch(bound ->
                safeEquals(bound.getMacAddress(), currentMac) &&
                        safeEquals(bound.getCpuSerial(), currentCpu) &&
                        safeEquals(bound.getMainBoardSerial(), currentBoard)
        );

        if (!match) {
            return Result.fail(4005, "硬件指纹不一致，当前机器非授权机器（cluster 模式）");
        }

        return Result.ok("机器指纹验证通过（cluster 模式）");
    }


    // 检测时间回拨并写入记录
    private Result<?> verifyClockRollback(String timeRecordPath) {
        try {
            long nowMillis = System.currentTimeMillis();
            Path recordPath = Paths.get(timeRecordPath);

            if (Files.exists(recordPath)) {
                String content = new String(Files.readAllBytes(recordPath), StandardCharsets.UTF_8).trim();
                String[] parts = content.split(":");

                if (parts.length != 2) {
                    return Result.fail(4006, "时间记录格式非法，可能被篡改");
                }

                String timestamp = parts[0];
                String hmacSignature = parts[1];

                String timeSecret = licenseConfig.getTimeSecret(); // 从配置中读取
                if (!HmacUtils.verify(timestamp, hmacSignature, timeSecret)) {
                    return Result.fail(4006, "检测到时间记录被篡改");
                }

                long lastStart = Long.parseLong(timestamp);
                if (nowMillis < lastStart) {
                    return Result.fail(4006, "检测到系统时间回拨，License 校验失败");
                }
            }

            // 写入新的记录
            String newRecord = nowMillis + ":" + HmacUtils.sign(String.valueOf(nowMillis), licenseConfig.getTimeSecret());
            Files.write(recordPath, newRecord.getBytes(StandardCharsets.UTF_8));
            return Result.ok("时间回拨检测通过");
        } catch (Exception e) {
            return Result.fail(5003, "时间回拨校验失败: " + e.getMessage());
        }
    }


    private void updateLicenseFirstUsedAt(String licensePath, LicenseContent license) {
        try {
            // 1. 清除签名字段
            license.setSignature(null);

            // 2. 将当前内容重新序列化
            String jsonToSign = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(license);

            // 3. 加载私钥
            PrivateKey privateKey = KeyStoreUtils.loadPrivateKeyFromJKS(
                    licenseConfig.getPrivateKey().getKeystorePath(),
                    licenseConfig.getPrivateKey().getAlias(),
                    licenseConfig.getPrivateKey().getStorePass(),
                    licenseConfig.getPrivateKey().getKeyPass()
            );

            // 4. 签名
            String newSignature = SignatureUtils.sign(jsonToSign, privateKey);
            license.setSignature(newSignature);

            // 5. 写回到文件
            String finalJson = objectMapper.writeValueAsString(license);
            Files.write(Paths.get(licensePath), finalJson.getBytes(StandardCharsets.UTF_8));

            log.info("已记录 License 首次使用时间: {}", license.getFirstUsedAt());
        } catch (Exception e) {
            log.warn("记录 firstUsedAt 失败（不影响主流程）", e);
        }
    }


    private boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
}
