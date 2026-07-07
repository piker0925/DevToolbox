package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ImageToPdfModule implements ToolModule {

    @Override
    public String getId() { return "image-to-pdf"; }

    @Override
    public String getName() { return "이미지 → PDF"; }

    @Override
    public String getCategory() { return "pdf"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        try {
            Path output = Files.createTempFile("img2pdf-", ".pdf");
            try (PDDocument doc = new PDDocument()) {
                for (Path imagePath : input.files()) {
                    PDImageXObject image = PDImageXObject.createFromFile(imagePath.toString(), doc);
                    PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                    doc.addPage(page);
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
                    }
                }
                doc.save(output.toFile());
            }
            return ToolResult.ofFile(output);
        } catch (IOException e) {
            throw new ToolProcessingException("이미지 → PDF 변환 실패: " + e.getMessage(), e);
        }
    }
}
