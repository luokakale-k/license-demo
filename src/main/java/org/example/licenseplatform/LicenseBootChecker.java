package org.example.licenseplatform.boot;

import lombok.extern.slf4j.Slf4j;
import org.example.licenseplatform.client.ClientLicenseConfig;
import org.example.licenseplatform.client.LicenseLoadException;
import org.example.licenseplatform.client.LicenseVerifier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * License 启动校验器：封装校验逻辑，清晰优雅地校验 License 并处理异常
 */
@Slf4j
public class LicenseBootChecker {

    /**
     * 启动并验证 License，校验失败时退出程序
     *
     * @param applicationClass SpringBootApplication 启动类
     */
    public static void run(Class<?> applicationClass) {
        try {
            // 启动 Spring 应用
            ConfigurableApplicationContext context = SpringApplication.run(applicationClass);

            // 获取配置与验证器
            ClientLicenseConfig config = context.getBean(ClientLicenseConfig.class);
            LicenseVerifier verifier = new LicenseVerifier(config);

            // 执行校验
            verifier.verify();

            log.info("License 校验通过，程序启动成功");

        } catch (LicenseLoadException e) {
            log.error("License 校验失败：" + e.getMessage());
            System.exit(1); // License 错误直接退出
        } catch (Exception e) {
            if (e.getClass().getName().contains("SilentExitException")) {
                // DevTools 的热重启，忽略
                return;
            }
            log.error("启动异常：{}", e.getMessage(), e);
            System.exit(2);
        }
    }
}
