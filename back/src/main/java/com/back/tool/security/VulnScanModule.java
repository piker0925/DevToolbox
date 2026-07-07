package com.back.tool.security;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VulnScanModule implements ToolModule {

    private static final Pattern GRADLE_DEP = Pattern.compile(
            "\"([^\"]+):([^\"]+):([^\"]+)\"");
    private static final Pattern MAVEN_DEP = Pattern.compile(
            "<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]+)</version>");

    private final String osvBaseUrl;

    public VulnScanModule() {
        this("https://api.osv.dev");
    }

    VulnScanModule(String osvBaseUrl) {
        this.osvBaseUrl = osvBaseUrl;
    }

    @Override
    public String getId() { return "vuln-scan"; }

    @Override
    public String getName() { return "의존성 취약점 스캔"; }

    @Override
    public String getCategory() { return "security"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        try {
            String content = Files.readString(input.files().get(0));
            String filename = input.files().get(0).getFileName().toString();
            boolean isMaven = filename.endsWith(".xml");

            List<Dependency> deps = isMaven ? parseMaven(content) : parseGradle(content);
            if (deps.isEmpty()) return ToolResult.ofText("파싱된 의존성이 없습니다.");

            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            StringBuilder report = new StringBuilder();
            report.append("스캔 대상: ").append(deps.size()).append("개 의존성\n\n");
            int vulnCount = 0;

            for (Dependency dep : deps) {
                String ecosystem = isMaven ? "Maven" : "Maven";
                String pkg = dep.groupId() + ":" + dep.artifactId();
                String body = """
                        {"version":"%s","package":{"name":"%s","ecosystem":"%s"}}
                        """.formatted(dep.version(), pkg, ecosystem).strip();

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(osvBaseUrl + "/v1/query"))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body().contains("\"id\"")) {
                    report.append("⚠️  ").append(pkg).append(":").append(dep.version())
                            .append(" — CVE 발견\n");
                    vulnCount++;
                }
            }

            if (vulnCount == 0) {
                report.append("✅ 스캔된 의존성에서 알려진 취약점이 없습니다.");
            } else {
                report.append("\n총 ").append(vulnCount).append("개 의존성에서 취약점 발견.");
            }
            return ToolResult.ofText(report.toString());
        } catch (Exception e) {
            throw new ToolProcessingException("취약점 스캔 실패: " + e.getMessage(), e);
        }
    }

    private List<Dependency> parseGradle(String content) {
        List<Dependency> deps = new ArrayList<>();
        Matcher m = GRADLE_DEP.matcher(content);
        while (m.find()) {
            String[] parts = m.group(0).replace("\"", "").split(":");
            if (parts.length == 3) deps.add(new Dependency(parts[0], parts[1], parts[2]));
        }
        return deps;
    }

    private List<Dependency> parseMaven(String content) {
        List<Dependency> deps = new ArrayList<>();
        Matcher m = MAVEN_DEP.matcher(content);
        while (m.find()) deps.add(new Dependency(m.group(1), m.group(2), m.group(3)));
        return deps;
    }

    private record Dependency(String groupId, String artifactId, String version) {}
}
