package com.back.tool.text;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class RegexModule implements ToolModule {

    @Override
    public String getId() { return "regex-tester"; }

    @Override
    public String getName() { return "정규식 테스터"; }

    @Override
    public String getCategory() { return "text"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String patternStr = input.params().getOrDefault("pattern", "");
        String text = input.params().getOrDefault("text", "");
        try {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);

            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(String.format("[%d-%d] %s", matcher.start(), matcher.end(), matcher.group()));
            }

            if (matches.isEmpty()) return ToolResult.ofText("매치 없음");
            return ToolResult.ofText(matches.size() + "개 매치:\n" + String.join("\n", matches));
        } catch (PatternSyntaxException e) {
            throw new ToolProcessingException("잘못된 정규식: " + e.getMessage(), e);
        }
    }
}
