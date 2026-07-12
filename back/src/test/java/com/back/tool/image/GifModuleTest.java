package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GifModuleTest {

    @TempDir
    Path tempDir;

    private final GifModule module = new GifModule();

    private Path createFrame(String name, Color color) throws Exception {
        return createFrame(name, color, 100, 100);
    }

    private Path createFrame(String name, Color color, int w, int h) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        ImageIO.write(img, "png", p.toFile());
        return p;
    }

    private List<Path> threeFrames() throws Exception {
        return List.of(
                createFrame("frame1.png", Color.RED),
                createFrame("frame2.png", Color.BLUE),
                createFrame("frame3.png", Color.GREEN));
    }

    /**
     * GIF 바이트에서 NETSCAPE 루프 카운트를 직접 파싱한다 (ImageIO 리더에 의존하지 않는 독립 검증).
     * 블록 구조: "NETSCAPE2.0" 다음 서브블록 {0x03, 0x01, loopLo, loopHi, 0x00}
     */
    private int readNetscapeLoopCount(Path gif) throws Exception {
        byte[] bytes = Files.readAllBytes(gif);
        byte[] sig = "NETSCAPE2.0".getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i <= bytes.length - sig.length - 5; i++) {
            for (int j = 0; j < sig.length; j++) {
                if (bytes[i + j] != sig[j]) continue outer;
            }
            int base = i + sig.length;
            assertThat(bytes[base]).isEqualTo((byte) 0x03);     // sub-block length
            assertThat(bytes[base + 1]).isEqualTo((byte) 0x01); // netscape sub-block id
            return (bytes[base + 2] & 0xFF) | ((bytes[base + 3] & 0xFF) << 8);
        }
        throw new AssertionError("NETSCAPE 확장 블록이 GIF에 없습니다");
    }

    private IIOMetadataNode firstFrameMetadata(Path gif) throws Exception {
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(gif.toFile())) {
            reader.setInput(iis);
            return (IIOMetadataNode) reader.getImageMetadata(0)
                    .getAsTree("javax_imageio_gif_image_1.0");
        } finally {
            reader.dispose();
        }
    }

    private IIOMetadataNode childNode(IIOMetadataNode root, String name) {
        NodeList nodes = root.getElementsByTagName(name);
        assertThat(nodes.getLength()).as("메타데이터 노드 %s 존재", name).isGreaterThan(0);
        return (IIOMetadataNode) nodes.item(0);
    }

    @Test
    void multipleFramesProduceGifFile() throws Exception {
        ToolResult result = module.process(new ToolInput(threeFrames(), Map.of("delay", "50")));

        assertThat(result.isFile()).isTrue();
        assertThat(result.outputFile()).exists();
        assertThat(result.outputFile().toString()).endsWith(".gif");

        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
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
    void delayWrittenAsCentisecondsInFrameMetadata() throws Exception {
        ToolResult result = module.process(new ToolInput(threeFrames(), Map.of("delay", "50")));

        IIOMetadataNode gce = childNode(firstFrameMetadata(result.outputFile()), "GraphicControlExtension");
        assertThat(gce.getAttribute("delayTime")).isEqualTo("5"); // 50ms = 5cs
    }

    @Test
    void defaultLoopCountIsInfinite() throws Exception {
        ToolResult result = module.process(new ToolInput(threeFrames(), Map.of("delay", "50")));

        assertThat(readNetscapeLoopCount(result.outputFile())).isEqualTo(0); // 0 = 무한
    }

    @Test
    void finiteLoopCountEncodedLittleEndian() throws Exception {
        ToolResult result = module.process(new ToolInput(threeFrames(),
                Map.of("delay", "50", "loopCount", "300"))); // 300 = 0x012C → lo=0x2C, hi=0x01

        assertThat(readNetscapeLoopCount(result.outputFile())).isEqualTo(300);
    }

    @Test
    void defaultDisposalIsRestoreToBackground() throws Exception {
        ToolResult result = module.process(new ToolInput(threeFrames(), Map.of("delay", "50")));

        IIOMetadataNode gce = childNode(firstFrameMetadata(result.outputFile()), "GraphicControlExtension");
        assertThat(gce.getAttribute("disposalMethod")).isEqualTo("restoreToBackgroundColor");
    }

    @Test
    void keepDisposalWritesDoNotDispose() throws Exception {
        ToolResult result = module.process(new ToolInput(threeFrames(),
                Map.of("delay", "50", "disposal", "keep")));

        IIOMetadataNode gce = childNode(firstFrameMetadata(result.outputFile()), "GraphicControlExtension");
        assertThat(gce.getAttribute("disposalMethod")).isEqualTo("doNotDispose");
    }

    @Test
    void invalidDisposalRejected() throws Exception {
        List<Path> frames = threeFrames();

        assertThatThrownBy(() -> module.process(new ToolInput(frames,
                Map.of("delay", "50", "disposal", "vanish"))))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("disposal");
    }

    @Test
    void frameSizeParamsScaleFramesKeepingAspectRatio() throws Exception {
        List<Path> frames = List.of(
                createFrame("wide1.png", Color.RED, 200, 100),
                createFrame("wide2.png", Color.BLUE, 200, 100));

        ToolResult result = module.process(new ToolInput(frames,
                Map.of("delay", "100", "frameWidth", "100", "frameHeight", "100")));

        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(result.outputFile().toFile())) {
            reader.setInput(iis);
            assertThat(reader.getNumImages(true)).isEqualTo(2);
            BufferedImage first = reader.read(0);
            assertThat(first.getWidth()).isEqualTo(100); // 200x100 → 100x100 박스 → 100x50
            assertThat(first.getHeight()).isEqualTo(50);
        } finally {
            reader.dispose();
        }
    }

    @Test
    void withoutFrameSizeParamsKeepsOriginalDimensions() throws Exception {
        List<Path> frames = List.of(
                createFrame("orig1.png", Color.RED, 200, 100),
                createFrame("orig2.png", Color.BLUE, 200, 100));

        ToolResult result = module.process(new ToolInput(frames, Map.of("delay", "100")));

        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(result.outputFile().toFile())) {
            reader.setInput(iis);
            BufferedImage first = reader.read(0);
            assertThat(first.getWidth()).isEqualTo(200);
            assertThat(first.getHeight()).isEqualTo(100);
        } finally {
            reader.dispose();
        }
    }

    @Test
    void moduleMetadata() {
        assertThat(module.getId()).isEqualTo("gif-create");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("image");
    }
}
