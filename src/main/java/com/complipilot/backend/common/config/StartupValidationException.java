package com.complipilot.backend.common.config;

public class StartupValidationException extends RuntimeException {

    public StartupValidationException(String message) {
        super(message);
    }
}