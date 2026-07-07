package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageResizeModuleTest {

    @TempDir
    Path tempDir;

    private Path createPng(String name, int w, int h) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", p.toFile());
        return p;
    }

    @Test
    void resizesToRequestedDimensions() throws Exception {
        Path src = createPng("input.png", 200, 150);

        ImageResizeModule module = new ImageResizeModule();
        ToolResult result = module.process(new ToolInput(List.of(src), Map.of("width", "100", "height", "75")));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        BufferedImage out = ImageIO.read(result.outputFile().toFile());
        assertThat(out.getWidth()).isEqualTo(100);
        assertThat(out.getHeight()).isEqualTo(75);
    }

    @Test
    void moduleMetadata() {
        ImageResizeModule module = new ImageResizeModule();
        assertThat(module.getId()).isEqualTo("image-resize");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("image");
    }
}
