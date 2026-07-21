package com.back.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

class RequiresFfmpegDrawtextCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isAvailable()) {
            return ConditionEvaluationResult.enabled("ffmpeg에 drawtext 필터가 있다");
        }
        return ConditionEvaluationResult.disabled(
                "이 ffmpeg 빌드에 drawtext 필터가 없다(libfreetype/libfontconfig 미포함) — "
                        + "docs/adr/0029 참조, `docker compose --profile test run --rm backend-native-test`로 검증할 것");
    }

    private boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-hide_banner", "-filters")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output.contains("drawtext");
        } catch (Exception e) {
            return false;
        }
    }
}
