package org.example.licenseplatform.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    PARAM_ERROR(400, "参数校验失败"),
    LICENSE_GEN_FAILED(500, "License 生成失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
