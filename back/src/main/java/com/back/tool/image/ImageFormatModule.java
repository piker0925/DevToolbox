package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolParams;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

@Component
public class ImageFormatModule implements ToolModule {

    @Override
    public String getId() { return "image-format"; }

    @Override
    public String getName() { return "이미지 포맷 변환"; }

    @Override
    public String getCategory() { return "image"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        ToolParams params = ToolParams.of(input);
        String targetFormat = params.getString("targetFormat", "png").toLowerCase();
        if (targetFormat.equals("jpg")) targetFormat = "jpeg";
        if (!targetFormat.equals("png") && !targetFormat.equals("jpeg")) {
            throw new ToolProcessingException("파라미터 'targetFormat'은 png 또는 jpg여야 합니다. (입력값: " + targetFormat + ")");
        }
        int quality = params.getInt("quality", 85, 1, 100);
        boolean progressive = params.getBool("progressive", false);
        boolean keepMetadata = params.getBool("keepMetadata", false);

        String ext = targetFormat.equals("jpeg") ? "jpg" : targetFormat;
        Path src = input.files().get(0);
        try {
            SourceImage source = readSource(src, keepMetadata);
            BufferedImage image = source.image();
            if (targetFormat.equals("jpeg")) {
                image = flattenAlpha(image);
            }
            // 메타데이터는 동일 포맷 재인코딩(png→png, jpg→jpg)일 때만 유지 가능하다.
            IIOMetadata metadata = (keepMetadata && targetFormat.equals(source.formatName()))
                    ? source.metadata() : null;

            Path output = Files.createTempFile("imgfmt-", "." + ext);
            write(image, targetFormat, output, quality, progressive, metadata);
            return ToolResult.ofFile(output);
        } catch (IOException e) {
            throw new ToolProcessingException("이미지 포맷 변환 실패: " + e.getMessage(), e);
        }
    }

    private record SourceImage(BufferedImage image, String formatName, IIOMetadata metadata) {}

    private SourceImage readSource(Path src, boolean withMetadata) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(src.toFile())) {
            if (stream == null) {
                throw new ToolProcessingException("이미지 파일을 읽을 수 없습니다: " + src.getFileName());
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new ToolProcessingException("지원하지 않는 이미지 형식입니다: " + src.getFileName());
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName().toLowerCase();
                if (format.equals("jpg")) format = "jpeg";
                BufferedImage image = reader.read(0);
                IIOMetadata metadata = withMetadata ? reader.getImageMetadata(0) : null;
                return new SourceImage(image, format, metadata);
            } finally {
                reader.dispose();
            }
        }
    }

    /** JPEG은 알파 채널을 지원하지 않으므로 투명 영역을 흰 배경으로 합성한다. */
    private BufferedImage flattenAlpha(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) return image;
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private void write(BufferedImage image, String format, Path output,
                       int quality, boolean progressive, IIOMetadata metadata) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        try (FileImageOutputStream stream = new FileImageOutputStream(output.toFile())) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (format.equals("jpeg")) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality / 100f);
                param.setProgressiveMode(progressive
                        ? ImageWriteParam.MODE_DEFAULT
                        : ImageWriteParam.MODE_DISABLED);
            }
            writer.write(null, new IIOImage(image, null, metadata), param);
        } finally {
            writer.dispose();
        }
    }
}
