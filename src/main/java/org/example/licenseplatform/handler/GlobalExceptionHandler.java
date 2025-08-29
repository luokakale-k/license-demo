package org.example.licenseplatform.handler;

import jakarta.validation.ConstraintViolationException;
import org.example.licenseplatform.common.ErrorCode;
import org.example.licenseplatform.common.Result;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), errorMessage);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return Result.fail(500, "系统异常: " + ex.getMessage());
    }
}
