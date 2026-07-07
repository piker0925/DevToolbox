package com.back.tool.text;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DiffModule implements ToolModule {

    @Override
    public String getId() { return "text-diff"; }

    @Override
    public String getName() { return "텍스트 Diff"; }

    @Override
    public String getCategory() { return "text"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        try {
            List<String> original = toLines(input.params().getOrDefault("original", ""));
            List<String> revised = toLines(input.params().getOrDefault("revised", ""));

            Patch<String> patch = DiffUtils.diff(original, revised);
            List<String> diff = UnifiedDiffUtils.generateUnifiedDiff("original", "revised", original, patch, 3);
            return ToolResult.ofText(diff.stream().collect(Collectors.joining("\n")));
        } catch (Exception e) {
            throw new ToolProcessingException("Diff 생성 실패: " + e.getMessage(), e);
        }
    }

    private List<String> toLines(String text) {
        if (text.isEmpty()) return List.of();
        return Arrays.asList(text.split("\n", -1));
    }
}
