package com.back.tool.model;

public interface ToolModule {
    String getId();

    String getName();

    String getCategory();

    boolean isHeavy();

    // true면 모든 파일을 단일 job으로 처리 (pdf-merge, gif-create)
    // false면 파일 1개당 별도 job (image-resize, image-format 등)
    default boolean acceptsMultipleFiles() { return false; }

    // 자원 등급 레인. 기본 HEAVY, 영상 모듈만 VIDEO로 오버라이드 (ADR-0019)
    default Lane getLane() { return Lane.HEAVY; }

    ToolResult process(ToolInput input);
}
