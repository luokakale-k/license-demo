package org.example.licenseplatform;

import org.example.licenseplatform.boot.LicenseBootChecker;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LicensePlatformApplication {
    public static void main(String[] args) {
        LicenseBootChecker.run(LicensePlatformApplication.class);
    }
}
