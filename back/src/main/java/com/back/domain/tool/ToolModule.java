package com.back.domain.tool;

public interface ToolModule {
    String getId();
    String getName();
    String getCategory();
    boolean isHeavy();
    ToolResult process(ToolInput input);
}
