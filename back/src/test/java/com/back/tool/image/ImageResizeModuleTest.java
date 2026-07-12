package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageResizeModuleTest {

    @TempDir
    Path tempDir;

    private final ImageResizeModule module = new ImageResizeModule();

    private Path createPng(String name, int w, int h) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", p.toFile());
        return p;
    }

    /** 픽셀마다 랜덤 색 — JPEG 품질 차이가 파일 크기에 드러나도록 하는 노이즈 이미지. */
    private Path createNoisyJpg(String name, int w, int h) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        ImageIO.write(img, "jpg", p.toFile());
        return p;
    }

    private BufferedImage run(Path src, Map<String, String> params) throws Exception {
        ToolResult result = module.process(new ToolInput(List.of(src), params));
        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        return ImageIO.read(result.outputFile().toFile());
    }

    @Test
    void resizesToRequestedDimensions() throws Exception {
        Path src = createPng("input.png", 200, 150);

        BufferedImage out = run(src, Map.of("width", "100", "height", "75"));

        assertThat(out.getWidth()).isEqualTo(100);
        assertThat(out.getHeight()).isEqualTo(75);
    }

    @Test
    void keepAspectRatioOnComputesHeightFromRatio() throws Exception {
        // 1000x800 (5:4) → 500x500 박스에 종횡비 유지로 맞추면 500x400
        Path src = createPng("ratio-on.png", 1000, 800);

        BufferedImage out = run(src, Map.of("width", "500", "height", "500", "keepAspectRatio", "true"));

        assertThat(out.getWidth()).isEqualTo(500);
        assertThat(out.getHeight()).isEqualTo(400);
    }

    @Test
    void keepAspectRatioOffForcesExactDimensions() throws Exception {
        // 같은 입력이라도 종횡비 잠금 해제 시 500x500으로 강제 변형된다
        Path src = createPng("ratio-off.png", 1000, 800);

        BufferedImage out = run(src, Map.of("width", "500", "height", "500", "keepAspectRatio", "false"));

        assertThat(out.getWidth()).isEqualTo(500);
        assertThat(out.getHeight()).isEqualTo(500);
    }

    @Test
    void percentUnitScalesFromOriginalDimensions() throws Exception {
        Path src = createPng("percent.png", 1000, 800);

        BufferedImage out = run(src, Map.of("unit", "%", "width", "50", "height", "50"));

        assertThat(out.getWidth()).isEqualTo(500);
        assertThat(out.getHeight()).isEqualTo(400);
    }

    @Test
    void upscaleReturnsWarningAdvisoryAlongsideFile() throws Exception {
        Path src = createPng("upscale.png", 200, 150);

        ToolResult result = module.process(new ToolInput(
                List.of(src), Map.of("unit", "%", "width", "200", "height", "200")));

        assertThat(result.isFile()).isTrue();
        BufferedImage out = ImageIO.read(result.outputFile().toFile());
        assertThat(out.getWidth()).isEqualTo(400);
        assertThat(out.getHeight()).isEqualTo(300);
        assertThat(result.textResult())
                .contains("경고")
                .contains("200x150")
                .contains("400x300");
    }

    @Test
    void downscaleHasNoAdvisory() throws Exception {
        Path src = createPng("downscale.png", 200, 150);

        ToolResult result = module.process(new ToolInput(
                List.of(src), Map.of("width", "100", "height", "75")));

        assertThat(result.textResult()).isNull();
    }

    @Test
    void jpegQualityLowersFileSize() throws Exception {
        Path src = createNoisyJpg("quality.jpg", 300, 300);

        ToolResult low = module.process(new ToolInput(List.of(src),
                Map.of("width", "300", "height", "300", "quality", "50")));
        ToolResult high = module.process(new ToolInput(List.of(src),
                Map.of("width", "300", "height", "300", "quality", "95")));

        // 둘 다 유효한 이미지 + 같은 크기여야 하고, 품질 50이 95보다 파일이 확실히 작아야 한다
        BufferedImage lowImg = ImageIO.read(low.outputFile().toFile());
        BufferedImage highImg = ImageIO.read(high.outputFile().toFile());
        assertThat(lowImg.getWidth()).isEqualTo(300);
        assertThat(highImg.getWidth()).isEqualTo(300);
        assertThat(low.outputFile().toFile().length())
                .isLessThan(high.outputFile().toFile().length());
    }

    @Test
    void invalidUnitRejected() throws Exception {
        Path src = createPng("bad-unit.png", 100, 100);

        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(src), Map.of("unit", "pt", "width", "50", "height", "50"))))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("unit");
    }

    @Test
    void moduleMetadata() {
        assertThat(module.getId()).isEqualTo("image-resize");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("image");
    }
}
