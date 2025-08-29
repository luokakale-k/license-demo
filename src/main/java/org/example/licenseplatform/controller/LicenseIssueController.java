package org.example.licenseplatform.controller;

import jakarta.validation.Valid;
import org.example.licenseplatform.common.ErrorCode;
import org.example.licenseplatform.common.Result;
import org.example.licenseplatform.model.LicenseRequest;
import org.example.licenseplatform.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/license")
public class LicenseIssueController {

    @Autowired
    private LicenseService licenseService;

    @PostMapping("/generate")
    public Result<Boolean> generateLicense(@Valid @RequestBody LicenseRequest request) {
        boolean success = licenseService.generateLicense(request);
        if (success) {
            return Result.ok(true);
        } else {
            return Result.fail(ErrorCode.LICENSE_GEN_FAILED.getCode(), ErrorCode.LICENSE_GEN_FAILED.getMessage());
        }
    }
}
