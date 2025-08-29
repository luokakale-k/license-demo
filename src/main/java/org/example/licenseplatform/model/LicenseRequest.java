package org.example.licenseplatform.model;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 客户端请求生成 License 时提交的参数模型
 */
@Data
public class LicenseRequest {

    @NotBlank(message = "licenseId 不能为空")
    private String licenseId;

    @NotBlank(message = "客户名称不能为空")
    private String customer;

    @NotBlank(message = "版本类型不能为空")
    private String edition;

    @NotNull(message = "起始时间不能为空")
    private Long issueDate;

    @NotNull(message = "到期时间不能为空")
    private Long expireDate;

    @NotNull(message = "功能模块不能为空")
    @Size(min = 1, message = "至少配置一个功能模块")
    private Map<String, Boolean> features;

    @NotNull(message = "机器指纹信息不能为空")
    @Size(min = 1, message = "至少绑定一台机器")
    private List<MachineInfo> boundMachines;

    /** 授权模式：standalone / cluster */
    @NotBlank(message = "授权模式不能为空")
    private String mode;
}
