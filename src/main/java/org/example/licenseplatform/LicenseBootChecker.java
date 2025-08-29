package org.example.licenseplatform.boot;

import lombok.extern.slf4j.Slf4j;
import org.example.licenseplatform.client.ClientLicenseConfig;
import org.example.licenseplatform.client.LicenseLoadException;
import org.example.licenseplatform.client.LicenseVerifier;
import org.example.licenseplatform.context.LicenseContext;
import org.example.licenseplatform.model.LicenseContent;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * License 启动校验器：封装 License 校验逻辑，优雅控制程序启动流程
 */
@Slf4j
public class LicenseBootChecker {

    /**
     * 启动 SpringBoot 应用，并执行 License 校验
     *
     * @param applicationClass 启动类
     */
    public static void run(Class<?> applicationClass) {
        try {
            // 1. 启动 Spring 容器
            ConfigurableApplicationContext context = SpringApplication.run(applicationClass);

            // 2. 获取客户端 License 配置与验证器
            ClientLicenseConfig config = context.getBean(ClientLicenseConfig.class);
            LicenseVerifier verifier = new LicenseVerifier(config);

            // 3. 执行 License 校验，返回授权内容
            //LicenseContent license = verifier.verify();

            // 4. 将授权状态注入 LicenseContext，全局可用
            //LicenseContext.setVerified(license);

            log.info("License 校验通过，程序启动成功");

        } catch (LicenseLoadException e) {
            log.error("License 校验失败：" + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            // DevTools 的热重启（不会触发完整启动流程），忽略
            if (e.getClass().getName().contains("SilentExitException")) {
                return;
            }
            log.error("启动异常：{}", e.getMessage(), e);
            System.exit(2);
        }
    }
}
