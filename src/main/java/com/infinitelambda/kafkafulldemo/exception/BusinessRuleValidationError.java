package com.infinitelambda.kafkafulldemo.exception;

public class BusinessRuleValidationError extends RuntimeException{
    public BusinessRuleValidationError(String message) {
        super(message);
    }
}
