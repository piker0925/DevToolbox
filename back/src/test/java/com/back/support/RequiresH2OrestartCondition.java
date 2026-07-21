package com.back.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

class RequiresH2OrestartCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (isInstalled()) {
            return ConditionEvaluationResult.enabled("H2Orestart(shared) 확장이 설치되어 있다");
        }
        return ConditionEvaluationResult.disabled(
                "LibreOffice에 H2Orestart(shared) 확장이 없어 HWP/HWPX를 열 수 없다 — "
                        + "docs/adr/0029 참조, `docker compose --profile test run --rm backend-native-test`로 검증할 것");
    }

    private boolean isInstalled() {
        try {
            Process process = new ProcessBuilder("unopkg", "list", "--shared")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output.contains("H2Orestart");
        } catch (Exception e) {
            return false;
        }
    }
}
