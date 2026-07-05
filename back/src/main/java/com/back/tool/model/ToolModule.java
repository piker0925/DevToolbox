package com.back.tool.model;

public interface ToolModule {
    String getId();

    String getName();

    String getCategory();

    boolean isHeavy();

    ToolResult process(ToolInput input);
}
