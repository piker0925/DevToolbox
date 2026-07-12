package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ImageFormatModuleTest {

    @TempDir
    Path tempDir;

    private final ImageFormatModule module = new ImageFormatModule();

    private String detectFormat(Path path) throws Exception {
        try (FileImageInputStream stream = new FileImageInputStream(path.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (readers.hasNext()) return readers.next().getFormatName().toLowerCase();
        }
        return "";
    }

    private Path createPng(String name, int w, int h) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", p.toFile());
        return p;
    }

    /** 픽셀마다 랜덤 색 — 품질 차이가 파일 크기에 드러나도록 하는 노이즈 이미지. */
    private Path createNoisyPng(String name, int w, int h) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(7);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        ImageIO.write(img, "png", p.toFile());
        return p;
    }

    /** tEXt 청크(키워드/값)를 포함한 PNG 생성 — 메타데이터 유지 여부 검증용. */
    private Path createPngWithTextMetadata(String name, String keyword, String value) throws Exception {
        Path p = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        try (FileImageOutputStream stream = new FileImageOutputStream(p.toFile())) {
            writer.setOutput(stream);
            IIOMetadata meta = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(img), null);
            String format = "javax_imageio_png_1.0";
            IIOMetadataNode root = new IIOMetadataNode(format);
            IIOMetadataNode text = new IIOMetadataNode("tEXt");
            IIOMetadataNode entry = new IIOMetadataNode("tEXtEntry");
            entry.setAttribute("keyword", keyword);
            entry.setAttribute("value", value);
            text.appendChild(entry);
            root.appendChild(text);
            meta.mergeTree(format, root);
            writer.write(null, new IIOImage(img, null, meta), null);
        } finally {
            writer.dispose();
        }
        return p;
    }

    private boolean containsBytes(Path file, byte[] needle) throws Exception {
        byte[] haystack = Files.readAllBytes(file);
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    /** JPEG SOF 마커 존재 여부. 엔트로피 데이터 내 0xFF 뒤에는 0x00/RST만 오므로 오탐 없음. */
    private boolean hasJpegMarker(Path file, int markerByte) throws Exception {
        return containsBytes(file, new byte[]{(byte) 0xFF, (byte) markerByte});
    }

    @Test
    void pngToJpg() throws Exception {
        Path src = createPng("input.png", 50, 50);

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

        ToolResult result = module.process(new ToolInput(List.of(src), Map.of("targetFormat", "png")));

        assertThat(result.outputFile().toString()).endsWith(".png");
        assertThat(detectFormat(result.outputFile())).containsIgnoringCase("png");
    }

    @Test
    void jpegQualityLowersFileSize() throws Exception {
        Path src = createNoisyPng("noisy.png", 300, 300);

        ToolResult low = module.process(new ToolInput(List.of(src),
                Map.of("targetFormat", "jpg", "quality", "50")));
        ToolResult high = module.process(new ToolInput(List.of(src),
                Map.of("targetFormat", "jpg", "quality", "95")));

        BufferedImage lowImg = ImageIO.read(low.outputFile().toFile());
        BufferedImage highImg = ImageIO.read(high.outputFile().toFile());
        assertThat(lowImg.getWidth()).isEqualTo(300);
        assertThat(highImg.getWidth()).isEqualTo(300);
        assertThat(low.outputFile().toFile().length())
                .isLessThan(high.outputFile().toFile().length());
    }

    @Test
    void progressiveOnWritesSof2Marker() throws Exception {
        Path src = createNoisyPng("prog.png", 100, 100);

        ToolResult result = module.process(new ToolInput(List.of(src),
                Map.of("targetFormat", "jpg", "progressive", "true")));

        // SOF2(0xFFC2) = progressive DCT, baseline SOF0(0xFFC0)은 없어야 한다
        assertThat(hasJpegMarker(result.outputFile(), 0xC2)).isTrue();
        assertThat(hasJpegMarker(result.outputFile(), 0xC0)).isFalse();
    }

    @Test
    void progressiveOffWritesBaselineSof0Marker() throws Exception {
        Path src = createNoisyPng("base.png", 100, 100);

        ToolResult result = module.process(new ToolInput(List.of(src),
                Map.of("targetFormat", "jpg", "progressive", "false")));

        assertThat(hasJpegMarker(result.outputFile(), 0xC0)).isTrue();
        assertThat(hasJpegMarker(result.outputFile(), 0xC2)).isFalse();
    }

    @Test
    void keepMetadataPreservesPngTextChunkOnSameFormat() throws Exception {
        Path src = createPngWithTextMetadata("meta-on.png", "Comment", "devtoolbox-exif-probe");
        byte[] probe = "devtoolbox-exif-probe".getBytes(StandardCharsets.ISO_8859_1);
        assertThat(containsBytes(src, probe)).isTrue(); // 원본에 실제로 들어있는지 자기 검증

        ToolResult result = module.process(new ToolInput(List.of(src),
                Map.of("targetFormat", "png", "keepMetadata", "true")));

        assertThat(detectFormat(result.outputFile())).containsIgnoringCase("png");
        assertThat(containsBytes(result.outputFile(), probe)).isTrue();
    }

    @Test
    void keepMetadataOffStripsPngTextChunk() throws Exception {
        Path src = createPngWithTextMetadata("meta-off.png", "Comment", "devtoolbox-exif-probe");
        byte[] probe = "devtoolbox-exif-probe".getBytes(StandardCharsets.ISO_8859_1);

        ToolResult result = module.process(new ToolInput(List.of(src),
                Map.of("targetFormat", "png", "keepMetadata", "false")));

        assertThat(containsBytes(result.outputFile(), probe)).isFalse();
    }

    @Test
    void keepMetadataAcrossFormatsDropsMetadataButSucceeds() throws Exception {
        Path src = createPngWithTextMetadata("meta-cross.png", "Comment", "devtoolbox-exif-probe");
        byte[] probe = "devtoolbox-exif-probe".getBytes(StandardCharsets.ISO_8859_1);

        ToolResult result = module.process(new ToolInput(List.of(src),
                Map.of("targetFormat", "jpg", "keepMetadata", "true")));

        assertThat(detectFormat(result.outputFile())).containsIgnoringCase("jpeg");
        BufferedImage out = ImageIO.read(result.outputFile().toFile());
        assertThat(out.getWidth()).isEqualTo(40);
        // keepMetadata=true여도 PNG→JPEG처럼 포맷이 바뀌면 원본 포맷 전용 메타데이터는 옮길 수 없다.
        assertThat(containsBytes(result.outputFile(), probe)).isFalse();
    }

    @Test
    void transparentPngFlattensToWhiteOnJpeg() throws Exception {
        Path src = tempDir.resolve("alpha.png");
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 0, 0, 0)); // 완전 투명
        g.fillRect(0, 0, 20, 20);
        g.dispose();
        ImageIO.write(img, "png", src.toFile());

        ToolResult result = module.process(new ToolInput(List.of(src), Map.of("targetFormat", "jpg")));

        BufferedImage out = ImageIO.read(result.outputFile().toFile());
        Color center = new Color(out.getRGB(10, 10));
        assertThat(center.getRed()).isGreaterThanOrEqualTo(240);
        assertThat(center.getGreen()).isGreaterThanOrEqualTo(240);
        assertThat(center.getBlue()).isGreaterThanOrEqualTo(240);
    }

    @Test
    void moduleMetadata() {
        assertThat(module.getId()).isEqualTo("image-format");
        assertThat(module.isHeavy()).isTrue();
        assertThat(module.getCategory()).isEqualTo("image");
    }
}
