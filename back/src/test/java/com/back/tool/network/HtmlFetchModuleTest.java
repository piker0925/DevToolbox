package com.back.tool.network;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlFetchModuleTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", exchange -> {
            byte[] body = "<html><body>Hello</body></html>".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void fetchesHtmlFromLocalServer() {
        HtmlFetchModule module = new HtmlFetchModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("url", "http://localhost:" + port + "/")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).contains("<html>");
        assertThat(result.textResult()).contains("Hello");
    }

    @Test
    void moduleMetadata() {
        HtmlFetchModule module = new HtmlFetchModule();
        assertThat(module.getId()).isEqualTo("html-fetch");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("network");
    }
}
