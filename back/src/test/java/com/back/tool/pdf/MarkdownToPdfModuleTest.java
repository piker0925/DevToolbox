package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownToPdfModuleTest {

    @TempDir
    Path tempDir;

    @Test
    void markdownFileConvertsToPdf() throws Exception {
        Path md = tempDir.resolve("test.md");
        Files.writeString(md, "# Hello\n\nThis is **markdown**.\n\n- item 1\n- item 2\n");

        MarkdownToPdfModule module = new MarkdownToPdfModule();
        ToolResult result = module.process(new ToolInput(List.of(md), Map.of()));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        assertThat(result.outputFile().toString()).endsWith(".pdf");
        try (PDDocument doc = PDDocument.load(result.outputFile().toFile())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Hello", "This is", "markdown", "item 1", "item 2");
        }
    }

    @Test
    void moduleMetadata() {
        MarkdownToPdfModule module = new MarkdownToPdfModule();
        assertThat(module.getId()).isEqualTo("markdown-to-pdf");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("pdf");
    }
}
