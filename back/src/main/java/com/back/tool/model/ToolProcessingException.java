package com.back.tool.model;

public class ToolProcessingException extends RuntimeException {
    public ToolProcessingException(String message) {
        super(message);
    }

    public ToolProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
