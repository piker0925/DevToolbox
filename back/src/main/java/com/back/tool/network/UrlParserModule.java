package com.back.tool.network;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UrlParserModule implements ToolModule {

    @Override
    public String getId() { return "url-parser"; }

    @Override
    public String getName() { return "URL 파서"; }

    @Override
    public String getCategory() { return "network"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String url = input.params().getOrDefault("url", "");
        try {
            URI uri = new URI(url);
            StringBuilder sb = new StringBuilder();
            sb.append("스킴:     ").append(uri.getScheme()).append("\n");
            sb.append("호스트:   ").append(uri.getHost()).append("\n");
            sb.append("포트:     ").append(uri.getPort() == -1 ? "(기본)" : uri.getPort()).append("\n");
            sb.append("경로:     ").append(uri.getPath()).append("\n");
            sb.append("프래그먼트: ").append(uri.getFragment() != null ? uri.getFragment() : "(없음)").append("\n");

            String query = uri.getRawQuery();
            if (query != null && !query.isEmpty()) {
                sb.append("쿼리 파라미터:\n");
                Arrays.stream(query.split("&")).forEach(pair -> {
                    int eq = pair.indexOf('=');
                    String k = decode(eq >= 0 ? pair.substring(0, eq) : pair);
                    String v = decode(eq >= 0 ? pair.substring(eq + 1) : "");
                    sb.append("  ").append(k).append(" = ").append(v).append("\n");
                });
            }
            return ToolResult.ofText(sb.toString().trim());
        } catch (Exception e) {
            throw new ToolProcessingException("URL 파싱 실패: " + e.getMessage(), e);
        }
    }

    private String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
