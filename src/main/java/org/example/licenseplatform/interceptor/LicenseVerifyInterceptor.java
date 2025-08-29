package org.example.licenseplatform.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.licenseplatform.context.LicenseContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * License 校验拦截器：用于在每个 HTTP 请求前进行 License 校验
 * 防止攻击者绕过 LicenseBootChecker 启动校验
 */
@Slf4j
@Component
public class LicenseVerifyInterceptor implements HandlerInterceptor {

    /**
     * 请求前执行：拦截未授权请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未通过授权校验，拒绝请求
        if (!LicenseContext.isVerified()) {
            log.warn("拒绝访问：未通过 License 授权，URI = {}", request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");

            try {
                response.getWriter().write("{\"code\":403, \"message\":\"未通过 License 授权，禁止访问\"}");
                response.getWriter().flush();
            } catch (Exception e) {
                log.error("响应写入失败", e);
            }

            return false;
        }

        // 已授权，正常放行
        return true;
    }
}
