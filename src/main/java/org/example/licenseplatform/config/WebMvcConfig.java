package org.example.licenseplatform.config;

import org.example.licenseplatform.interceptor.LicenseVerifyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LicenseVerifyInterceptor licenseVerifyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licenseVerifyInterceptor)
                .addPathPatterns("/**"); // 可自定义排除
    }
}
