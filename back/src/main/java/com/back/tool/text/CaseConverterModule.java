package com.back.tool.text;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import com.google.common.base.CaseFormat;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CaseConverterModule implements ToolModule {

    private static final Map<String, CaseFormat> FORMATS = Map.of(
            "camel", CaseFormat.LOWER_CAMEL,
            "pascal", CaseFormat.UPPER_CAMEL,
            "snake", CaseFormat.LOWER_UNDERSCORE,
            "kebab", CaseFormat.LOWER_HYPHEN
    );

    @Override
    public String getId() { return "case-converter"; }

    @Override
    public String getName() { return "케이스 변환기"; }

    @Override
    public String getCategory() { return "text"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String text = input.params().getOrDefault("text", "");
        String from = input.params().getOrDefault("from", "camel");
        String to = input.params().getOrDefault("to", "snake");

        CaseFormat fromFmt = FORMATS.get(from);
        CaseFormat toFmt = FORMATS.get(to);
        if (fromFmt == null || toFmt == null) {
            throw new ToolProcessingException("지원하지 않는 케이스 형식: from=" + from + " to=" + to, null);
        }
        return ToolResult.ofText(fromFmt.to(toFmt, text));
    }
}
