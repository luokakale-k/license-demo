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
     * 验证 License 签名是否合法
     * @param license 被校验的 LicenseContent
     * @param publicKey 公钥（由服务端生成）
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

            license.setSignature(signature); // 验签后恢复原值
        } catch (Exception e) {
            throw new LicenseLoadException("签名验证出错", e);
        }
    }

    /**
     * 验证 License 的时间是否合法（已生效 + 未过期）
     * @param license LicenseContent 对象
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
     * 校验当前机器是否符合 License 授权的硬件指纹
     * 区分 standalone（单机） 与 cluster（集群） 模式
     * @param license LicenseContent 对象
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
     * 校验首次使用时间合法性（不能小于签发时间）
     */
    public static void validateFirstUsedAt(LicenseContent license) {
        Long firstUsedAt = license.getFirstUsedAt();
        if (firstUsedAt == null) {
            throw new LicenseLoadException("首次使用时间为空，License 文件可能不完整");
        }

        long now = System.currentTimeMillis();
        if (now < firstUsedAt) {
            throw new LicenseLoadException("系统时间早于首次使用时间，可能存在时间回拨风险");
        }
    }

    /**
     * 检查系统是否存在时间回拨（比上次运行更早）
     * 采用 HMAC 加密记录方式防止被恶意伪造
     *
     * @param timeRecordPath 本地记录路径
     * @param timeSecret HMAC 使用的密钥
     * @param firstUsedAt License 中记录的首次使用时间（毫秒）
     * @param allowedDeployDelaySeconds 允许误差秒数（部署启动窗口）
     */
    public static void validateTimeRollback(String timeRecordPath, String timeSecret, long firstUsedAt, int allowedDeployDelaySeconds) {
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

            // 首次部署启动，判断是否在允许窗口内
            long delayMillis = allowedDeployDelaySeconds * 1000L;
            if (Math.abs(now - firstUsedAt) > delayMillis) {
                throw new LicenseLoadException("时间记录文件缺失，且当前时间与首次使用时间差距过大，疑似被删除或系统被回拨");
            }

            // 写入最新时间戳
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

    /**
     * 安全字符串比较，防止空指针
     */
    private static boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

}
