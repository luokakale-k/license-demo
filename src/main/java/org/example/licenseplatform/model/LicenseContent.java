package org.example.licenseplatform.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * License 授权文件内容模型（最终写入 .lic 文件）
 * 包含了客户信息、授权时间范围、功能开关、绑定机器列表、授权模式和签名字段
 */
@Data
public class LicenseContent {

    /** 授权编号，全局唯一，用于内部追踪 */
    private String licenseId;

    /** 客户名称或公司名，用于识别客户身份 */
    private String customer;

    /** 授权版本，例如 basic、pro、enterprise 等 */
    private String edition;

    /** 授权生效时间（毫秒时间戳） */
    private Long issueDate;

    /** 授权过期时间（毫秒时间戳） */
    private Long expireDate;

    /** 授权功能模块配置 */
    private Map<String, Boolean> features;

    /** 多台绑定机器信息，用于集群部署识别 */
    private List<MachineInfo> boundMachines;

    /** 授权模式（standalone / cluster），用于行为控制 */
    private String mode;

    /** 首次使用时间（毫秒时间戳），用于记录首次加载并防止复制横向扩散 */
    private Long firstUsedAt;

    /** 签名字段（私钥签名后的密文，防止篡改） */
    private String signature;
}
