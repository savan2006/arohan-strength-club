package com.arohan.strengthclub.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public BusinessRuleException(String code, String message, HttpStatus status) {
        super(message); this.code = code; this.status = status;
    }
    public BusinessRuleException(String code, String message) { this(code, message, HttpStatus.BAD_REQUEST); }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
