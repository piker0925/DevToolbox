package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageToPdfModuleTest {

    @TempDir
    Path tempDir;

    @Test
    void singleImageConvertsToPdf() throws Exception {
        Path imgPath = tempDir.resolve("test.jpg");
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        ImageIO.write(img, "jpg", imgPath.toFile());

        ImageToPdfModule module = new ImageToPdfModule();
        ToolResult result = module.process(new ToolInput(List.of(imgPath), Map.of()));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        assertThat(result.outputFile().toString()).endsWith(".pdf");
        try (PDDocument doc = PDDocument.load(result.outputFile().toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void multipleImagesProduceMultiPagePdf() throws Exception {
        Path img1 = tempDir.resolve("a.png");
        Path img2 = tempDir.resolve("b.png");
        for (Path p : List.of(img1, img2)) {
            BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(img, "png", p.toFile());
        }

        ImageToPdfModule module = new ImageToPdfModule();
        ToolResult result = module.process(new ToolInput(List.of(img1, img2), Map.of()));

        try (PDDocument doc = PDDocument.load(result.outputFile().toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    void acceptsMultipleFilesAsOneJob() {
        // 컨트롤러가 여러 파일을 하나의 job으로 넘겨야 여러 이미지가 한 PDF로 합쳐진다.
        // 이 값이 false면 파일마다 별도 배치 job으로 쪼개져 1페이지짜리 PDF가 여러 개 생성된다.
        ImageToPdfModule module = new ImageToPdfModule();
        assertThat(module.acceptsMultipleFiles()).isTrue();
    }

    @Test
    void moduleMetadata() {
        ImageToPdfModule module = new ImageToPdfModule();
        assertThat(module.getId()).isEqualTo("image-to-pdf");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("pdf");
    }
}
