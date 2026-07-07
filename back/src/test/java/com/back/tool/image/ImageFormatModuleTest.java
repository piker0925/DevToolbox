package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageFormatModuleTest {

    @TempDir
    Path tempDir;

    private String detectFormat(Path path) throws Exception {
        try (FileImageInputStream stream = new FileImageInputStream(path.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (readers.hasNext()) return readers.next().getFormatName().toLowerCase();
        }
        return "";
    }

    @Test
    void pngToJpg() throws Exception {
        Path src = tempDir.resolve("input.png");
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", src.toFile());

        ImageFormatModule module = new ImageFormatModule();
        ToolResult result = module.process(new ToolInput(List.of(src), Map.of("targetFormat", "jpg")));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile().toString()).endsWith(".jpg");
        assertThat(detectFormat(result.outputFile())).containsIgnoringCase("jpeg");
    }

    @Test
    void jpgToPng() throws Exception {
        Path src = tempDir.resolve("input.jpg");
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "jpg", src.toFile());

        ImageFormatModule module = new ImageFormatModule();
        ToolResult result = module.process(new ToolInput(List.of(src), Map.of("targetFormat", "png")));

        assertThat(result.outputFile().toString()).endsWith(".png");
        assertThat(detectFormat(result.outputFile())).containsIgnoringCase("png");
    }

    @Test
    void moduleMetadata() {
        ImageFormatModule module = new ImageFormatModule();
        assertThat(module.getId()).isEqualTo("image-format");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("image");
    }
}
