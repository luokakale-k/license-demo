package org.example.licenseplatform.context;

import org.example.licenseplatform.model.LicenseContent;

import java.util.Map;

/**
 * LicenseContext 是 License 校验通过后全局缓存授权状态的上下文工具类。
 * 可用于在系统任意位置判断是否通过授权、当前授权内容、功能是否启用等信息。
 *
 * ⚠ 注意：LicenseContext 一般由 LicenseVerifier 在校验通过后注入初始化。
 */
public class LicenseContext {

    /** 标识当前系统是否通过 License 校验（默认 false） */
    private static volatile boolean verified = false;

    /** 全局缓存的 License 内容（包括功能模块、客户信息等） */
    private static LicenseContent license;

    /**
     * 校验通过后，注入授权状态和授权内容
     *
     * @param content 校验后的 LicenseContent 内容
     */
    public static void setVerified(LicenseContent content) {
        verified = true;
        license = content;
    }

    /**
     * 获取当前是否通过 License 校验
     *
     * @return true 表示校验通过
     */
    public static boolean isVerified() {
        return verified;
    }

    /**
     * 获取当前缓存的 License 内容对象（包含授权编号、客户名、功能等）
     *
     * @return LicenseContent 对象
     */
    public static LicenseContent getLicense() {
        return license;
    }

    /**
     * 判断某功能模块是否启用
     *
     * @param featureKey 功能模块名（如 exportExcel）
     * @return true 表示已授权该功能
     */
    public static boolean isFeatureEnabled(String featureKey) {
        if (!verified || license == null || license.getFeatures() == null) {
            return false;
        }

        Boolean enabled = license.getFeatures().get(featureKey);
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 获取某个功能模块的所有配置（适用于功能扩展为复杂结构时）
     *
     * @param featureKey 功能模块名
     * @return Object 对象，可自行强转为 FeatureSetting 或 Map
     */
    public static Object getFeatureRaw(String featureKey) {
        if (!verified || license == null || license.getFeatures() == null) {
            return null;
        }
        return license.getFeatures().get(featureKey);
    }

    /**
     * 获取所有功能配置 Map（如 exportExcel -> true）
     *
     * @return Map<String, Boolean>
     */
    public static Map<String, Boolean> getAllFeatures() {
        if (!verified || license == null) return null;
        return license.getFeatures();
    }

    /**
     * 清空上下文（用于测试或重新加载 License）
     */
    public static void reset() {
        verified = false;
        license = null;
    }
}
