package com.back.tool.security;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VulnScanModuleTest {

    @TempDir
    Path tempDir;

    HttpServer server;
    String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void reportsVulnerabilityWhenOsvReturnsId() throws Exception {
        server.createContext("/v1/query", exchange -> {
            byte[] body = "{\"vulns\":[{\"id\":\"CVE-2021-44228\"}]}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();

        Path gradle = tempDir.resolve("build.gradle");
        Files.writeString(gradle, """
                dependencies {
                    implementation("log4j:log4j:1.2.17")
                }
                """);

        VulnScanModule module = new VulnScanModule(baseUrl);
        ToolResult result = module.process(new ToolInput(List.of(gradle), Map.of()));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).contains("CVE 발견");
    }

    @Test
    void reportsCleanWhenOsvReturnsNoVulnerabilities() throws Exception {
        server.createContext("/v1/query", exchange -> {
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();

        Path gradle = tempDir.resolve("build.gradle");
        Files.writeString(gradle, """
                dependencies {
                    implementation("com.example:safe-lib:1.0.0")
                }
                """);

        VulnScanModule module = new VulnScanModule(baseUrl);
        ToolResult result = module.process(new ToolInput(List.of(gradle), Map.of()));

        assertThat(result.textResult()).contains("취약점이 없습니다");
    }

    @Test
    void returnsEarlyWhenNoDependenciesParsed() throws Exception {
        server.start();

        Path gradle = tempDir.resolve("build.gradle");
        Files.writeString(gradle, "// 의존성 없음");

        VulnScanModule module = new VulnScanModule(baseUrl);
        ToolResult result = module.process(new ToolInput(List.of(gradle), Map.of()));

        assertThat(result.textResult()).contains("파싱된 의존성이 없습니다");
    }

    @Test
    void parsesMavenPomFile() throws Exception {
        server.createContext("/v1/query", exchange -> {
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();

        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
                <dependencies>
                  <dependency>
                    <groupId>org.apache.commons</groupId>
                    <artifactId>commons-lang3</artifactId>
                    <version>3.12.0</version>
                  </dependency>
                </dependencies>
                """);

        VulnScanModule module = new VulnScanModule(baseUrl);
        ToolResult result = module.process(new ToolInput(List.of(pom), Map.of()));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).contains("스캔 대상: 1개");
    }

    @Test
    void moduleMetadata() {
        VulnScanModule module = new VulnScanModule();
        assertThat(module.getId()).isEqualTo("vuln-scan");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("security");
    }
}
