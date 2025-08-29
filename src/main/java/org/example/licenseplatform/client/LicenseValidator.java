package org.example.licenseplatform.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.licenseplatform.model.LicenseContent;
import org.example.licenseplatform.model.MachineInfo;
import org.example.licenseplatform.util.HmacUtils;
import org.example.licenseplatform.util.JsonUtils;
import org.example.licenseplatform.util.MachineInfoUtils;
import org.example.licenseplatform.util.SignatureUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

public class LicenseValidator {

    private static final ObjectMapper objectMapper =  JsonUtils.getMapper();

    /**
     * 验证签名是否合法
     */
    public static void validateSignature(LicenseContent license, PublicKey publicKey) {
        try {
            // 清除签名字段，重新计算签名前的 JSON 字符串
            String signature = license.getSignature();
            license.setSignature(null);

            String rawJson = objectMapper.writeValueAsString(license);
            if (!SignatureUtils.verify(rawJson, signature, publicKey)) {
                throw new LicenseLoadException("签名验证失败，License 非法或被篡改");
            }

            // 还原签名
            license.setSignature(signature);
        } catch (Exception e) {
            throw new LicenseLoadException("签名验证出错", e);
        }
    }

    /**
     * 验证时间是否合法（未过期、已生效）
     */
    public static void validateDate(LicenseContent license) {
        try {
            long now = System.currentTimeMillis();
            long issueTime = license.getIssueDate();
            long expireTime = license.getExpireDate();

            if (now < issueTime) {
                throw new LicenseLoadException("License 尚未生效");
            }
            if (now > expireTime) {
                throw new LicenseLoadException("License 已过期");
            }
        } catch (Exception e) {
            throw new LicenseLoadException("时间格式非法或校验异常：" + e.getMessage(), e);
        }
    }

    /**
     * 验证当前机器指纹是否符合 License 授权硬件信息
     * 区分 standalone / cluster 模式
     */
    public static void validateHardware(LicenseContent license) {
        if (license.getBoundMachines() == null || license.getBoundMachines().isEmpty()) {
            throw new LicenseLoadException("License 中未配置绑定机器信息");
        }

        MachineInfo current = MachineInfoUtils.getMachineInfo();
        String mode = license.getMode();

        if ("standalone".equalsIgnoreCase(mode)) {
            // 单机模式只比对第一台
            MachineInfo only = license.getBoundMachines().get(0);
            if (!safeEquals(only.getMacAddress(), current.getMacAddress()) ||
                    !safeEquals(only.getCpuSerial(), current.getCpuSerial()) ||
                    !safeEquals(only.getMainBoardSerial(), current.getMainBoardSerial())) {
                throw new LicenseLoadException("当前机器与授权机器不一致，License 校验失败（standalone 模式）");
            }
            return;
        }

        // cluster 模式：遍历任意一台机器
        boolean matched = license.getBoundMachines().stream().anyMatch(bound ->
                safeEquals(bound.getMacAddress(), current.getMacAddress()) &&
                        safeEquals(bound.getCpuSerial(), current.getCpuSerial()) &&
                        safeEquals(bound.getMainBoardSerial(), current.getMainBoardSerial())
        );

        if (!matched) {
            throw new LicenseLoadException("当前机器不在授权列表中，License 校验失败（cluster 模式）");
        }
    }


    /**
     * 安全比较，避免 NPE
     */
    private static boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    /**
     * 时间回拨检测：当前时间不能比上次启动时间早
     */
    public static void validateTimeRollback(String timeRecordPath, String timeSecret) {
        try {
            long now = System.currentTimeMillis();
            Path recordPath = Paths.get(timeRecordPath);

            if (Files.exists(recordPath)) {
                String content = new String(Files.readAllBytes(recordPath), StandardCharsets.UTF_8).trim();
                String[] parts = content.split(":");

                if (parts.length != 2) {
                    throw new LicenseLoadException("时间记录格式非法，可能被篡改");
                }

                String timestamp = parts[0];
                String hmac = parts[1];

                if (!HmacUtils.verify(timestamp, hmac, timeSecret)) {
                    throw new LicenseLoadException("检测到时间记录被篡改");
                }

                long last = Long.parseLong(timestamp);
                if (now < last) {
                    throw new LicenseLoadException("检测到系统时间回拨，License 校验失败");
                }
            }

            String newRecord = now + ":" + HmacUtils.sign(String.valueOf(now), timeSecret);
            Files.write(recordPath, newRecord.getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            throw new LicenseLoadException("时间回拨检测失败（文件IO异常）", e);
        } catch (NumberFormatException e) {
            throw new LicenseLoadException("时间回拨检测失败（时间格式异常）", e);
        } catch (Exception e) {
            throw new LicenseLoadException("时间回拨检测失败", e);
        }
    }


}
