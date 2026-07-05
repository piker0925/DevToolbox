package com.back.domain.tool;

public class ToolProcessingException extends RuntimeException {
    public ToolProcessingException(String message) {
        super(message);
    }

    public ToolProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
