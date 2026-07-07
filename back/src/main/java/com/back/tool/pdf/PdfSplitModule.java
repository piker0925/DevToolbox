package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class PdfSplitModule implements ToolModule {

    @Override
    public String getId() { return "pdf-split"; }

    @Override
    public String getName() { return "PDF 분할"; }

    @Override
    public String getCategory() { return "pdf"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        try {
            Path inputPdf = input.files().get(0);
            Path output = Files.createTempFile("pdfsplit-", ".zip");

            try (PDDocument doc = PDDocument.load(inputPdf.toFile())) {
                List<PDDocument> pages = new Splitter().split(doc);
                try (OutputStream fos = Files.newOutputStream(output);
                     ZipOutputStream zip = new ZipOutputStream(fos)) {
                    for (int i = 0; i < pages.size(); i++) {
                        PDDocument page = pages.get(i);
                        Path pagePath = Files.createTempFile("page-", ".pdf");
                        try {
                            page.save(pagePath.toFile());
                            zip.putNextEntry(new ZipEntry(String.format("page-%03d.pdf", i + 1)));
                            Files.copy(pagePath, zip);
                            zip.closeEntry();
                        } finally {
                            page.close();
                            Files.deleteIfExists(pagePath);
                        }
                    }
                }
            }
            return ToolResult.ofFile(output);
        } catch (IOException e) {
            throw new ToolProcessingException("PDF 분할 실패: " + e.getMessage(), e);
        }
    }
}
