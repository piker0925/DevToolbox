package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class MarkdownToPdfModule implements ToolModule {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    @Override
    public String getId() { return "markdown-to-pdf"; }

    @Override
    public String getName() { return "마크다운 → PDF"; }

    @Override
    public String getCategory() { return "pdf"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        try {
            String markdown = Files.readString(input.files().get(0));
            Node document = PARSER.parse(markdown);
            String body = RENDERER.render(document);
            String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>"
                    + "<style>body{font-family:sans-serif;margin:40px;line-height:1.6}"
                    + "pre{background:#f4f4f4;padding:12px;border-radius:4px}"
                    + "code{font-family:monospace}</style></head><body>"
                    + body + "</body></html>";

            Path output = Files.createTempFile("md2pdf-", ".pdf");
            try (OutputStream os = Files.newOutputStream(output)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.withHtmlContent(html, null);
                builder.toStream(os);
                builder.run();
            }
            return ToolResult.ofFile(output);
        } catch (Exception e) {
            throw new ToolProcessingException("마크다운 → PDF 변환 실패: " + e.getMessage(), e);
        }
    }
}
