package com.back.tool.model;

import java.nio.file.Path;

public record ToolResult(Path outputFile, String textResult) {
    public static ToolResult ofFile(Path path) {
        return new ToolResult(path, null);
    }

    public static ToolResult ofText(String text) {
        return new ToolResult(null, text);
    }

    public boolean isFile() {
        return outputFile != null;
    }
}
