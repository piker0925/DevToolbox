package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

class PdfSplitModuleTest {

    @TempDir
    Path tempDir;

    private Path createPdf(int pages) throws Exception {
        Path path = tempDir.resolve("input.pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) doc.addPage(new PDPage());
            doc.save(path.toFile());
        }
        return path;
    }

    @Test
    void splitThreePagePdfProducesZipWithThreeEntries() throws Exception {
        Path pdf = createPdf(3);

        PdfSplitModule module = new PdfSplitModule();
        ToolResult result = module.process(new ToolInput(List.of(pdf), Map.of()));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        assertThat(result.outputFile().toString()).endsWith(".zip");
        try (ZipFile zip = new ZipFile(result.outputFile().toFile())) {
            assertThat(zip.size()).isEqualTo(3);
        }
    }

    @Test
    void moduleMetadata() {
        PdfSplitModule module = new PdfSplitModule();
        assertThat(module.getId()).isEqualTo("pdf-split");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("pdf");
    }
}
