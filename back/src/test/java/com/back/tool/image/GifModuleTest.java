package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GifModuleTest {

    @TempDir
    Path tempDir;

    private Path createFrame(String name, Color color) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        ImageIO.write(img, "png", p.toFile());
        return p;
    }

    @Test
    void multipleFramesProduceGifFile() throws Exception {
        Path f1 = createFrame("frame1.png", Color.RED);
        Path f2 = createFrame("frame2.png", Color.BLUE);
        Path f3 = createFrame("frame3.png", Color.GREEN);

        GifModule module = new GifModule();
        ToolResult result = module.process(new ToolInput(
                List.of(f1, f2, f3),
                Map.of("delay", "50")
        ));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        assertThat(result.outputFile().toString()).endsWith(".gif");
        assertThat(result.outputFile().toFile().length()).isGreaterThan(0);

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(result.outputFile().toFile())) {
            reader.setInput(iis);
            assertThat(reader.getNumImages(true)).isEqualTo(3);

            assertThat(dominantColor(reader.read(0))).isEqualTo(Color.RED);
            assertThat(dominantColor(reader.read(1))).isEqualTo(Color.BLUE);
            assertThat(dominantColor(reader.read(2))).isEqualTo(Color.GREEN);
        } finally {
            reader.dispose();
        }
    }

    private Color dominantColor(BufferedImage frame) {
        return new Color(frame.getRGB(frame.getWidth() / 2, frame.getHeight() / 2));
    }

    @Test
    void moduleMetadata() {
        GifModule module = new GifModule();
        assertThat(module.getId()).isEqualTo("gif-create");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("image");
    }
}
