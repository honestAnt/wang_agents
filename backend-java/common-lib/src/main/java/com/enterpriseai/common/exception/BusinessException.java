package com.enterpriseai.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BusinessException notFound(String resource, String id) {
        return new BusinessException(404, resource + " not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message, HttpStatus.BAD_REQUEST);
    }
}
