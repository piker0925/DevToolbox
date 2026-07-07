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

import static org.assertj.core.api.Assertions.assertThat;

class PdfMergeModuleTest {

    @TempDir
    Path tempDir;

    private Path createPdf(String name, int pages) throws Exception {
        Path path = tempDir.resolve(name);
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) doc.addPage(new PDPage());
            doc.save(path.toFile());
        }
        return path;
    }

    @Test
    void mergeTwoPdfsProducesCombinedPageCount() throws Exception {
        Path pdf1 = createPdf("a.pdf", 2);
        Path pdf2 = createPdf("b.pdf", 3);

        PdfMergeModule module = new PdfMergeModule();
        ToolResult result = module.process(new ToolInput(List.of(pdf1, pdf2), Map.of()));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        try (PDDocument doc = PDDocument.load(result.outputFile().toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(5);
        }
    }

    @Test
    void moduleMetadata() {
        PdfMergeModule module = new PdfMergeModule();
        assertThat(module.getId()).isEqualTo("pdf-merge");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("pdf");
    }
}
