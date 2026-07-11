package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

class PdfSplitModuleTest {

    @TempDir
    Path tempDir;

    private Path createPdf(String... pageLabels) throws Exception {
        Path path = tempDir.resolve("input.pdf");
        try (PDDocument doc = new PDDocument()) {
            for (String label : pageLabels) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 24);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(label);
                    cs.endText();
                }
            }
            doc.save(path.toFile());
        }
        return path;
    }

    @Test
    void splitThreePagePdfProducesZipWithThreeEntries() throws Exception {
        Path pdf = createPdf("P1", "P2", "P3");

        PdfSplitModule module = new PdfSplitModule();
        ToolResult result = module.process(new ToolInput(List.of(pdf), Map.of()));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        assertThat(result.outputFile().toString()).endsWith(".zip");
        try (ZipFile zip = new ZipFile(result.outputFile().toFile())) {
            assertThat(zip.size()).isEqualTo(3);

            String[] expectedLabels = {"P1", "P2", "P3"};
            for (int i = 0; i < expectedLabels.length; i++) {
                String entryName = String.format("page-%03d.pdf", i + 1);
                var entry = zip.getEntry(entryName);
                assertThat(entry).as("entry %s should exist", entryName).isNotNull();

                try (InputStream in = zip.getInputStream(entry);
                     PDDocument page = PDDocument.load(in)) {
                    assertThat(page.getNumberOfPages()).isEqualTo(1);
                    assertThat(new PDFTextStripper().getText(page)).contains(expectedLabels[i]);
                }
            }
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
