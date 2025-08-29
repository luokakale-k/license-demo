package org.example.licenseplatform.client;

/**
 * License 加载或校验失败时抛出，附带错误提示与可扩展错误码
 */
public class LicenseLoadException extends RuntimeException {

    public LicenseLoadException(String message) {
        super(message);
    }

    public LicenseLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
