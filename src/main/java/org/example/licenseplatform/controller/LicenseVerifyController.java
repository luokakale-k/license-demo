// LicenseVerifyController.java
package org.example.licenseplatform.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.licenseplatform.common.Result;
import org.example.licenseplatform.service.LicenseVerifierService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/license")
@Slf4j
public class LicenseVerifyController {

    private final LicenseVerifierService verifierService;

    public LicenseVerifyController(LicenseVerifierService verifierService) {
        this.verifierService = verifierService;
    }

    @Value("${license.client.public-key-path}")
    private String publicKeyPath;

    @Value("${license.client.time-record-path}")
    private String timeRecordPath;

    @GetMapping("/verify")
    public Result verify(@RequestParam String licensePath) {
        log.info("开始验证 License: {}", licensePath);
        return verifierService.verify(licensePath, publicKeyPath, timeRecordPath);
    }
}
