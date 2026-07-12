package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolParams;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class GifModule implements ToolModule {

    @Override
    public String getId() { return "gif-create"; }

    @Override
    public String getName() { return "GIF 생성"; }

    @Override
    public String getCategory() { return "image"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public boolean acceptsMultipleFiles() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        ToolParams params = ToolParams.of(input);
        int delayMs = params.getInt("delay", 100, 10, 60000);
        int delayCs = delayMs / 10; // centiseconds
        int loopCount = params.getInt("loopCount", 0, 0, 65535); // 0 = 무한 반복
        String disposalMethod = resolveDisposal(params.getString("disposal", "background"));
        int frameWidth = params.getInt("frameWidth", 0, 0, 10000);   // 0 = 원본 크기
        int frameHeight = params.getInt("frameHeight", 0, 0, 10000); // 0 = 원본 크기

        try {
            Path output = Files.createTempFile("gif-", ".gif");
            ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();

            try (FileImageOutputStream stream = new FileImageOutputStream(output.toFile())) {
                writer.setOutput(stream);
                writer.prepareWriteSequence(null);

                for (Path framePath : input.files()) {
                    BufferedImage frame = ImageIO.read(framePath.toFile());
                    if (frame == null) {
                        throw new ToolProcessingException("이미지 파일을 읽을 수 없습니다: " + framePath.getFileName());
                    }
                    frame = scaleFrame(frame, frameWidth, frameHeight);
                    IIOMetadata meta = buildFrameMetadata(writer, frame, delayCs, loopCount, disposalMethod);
                    writer.writeToSequence(new IIOImage(frame, null, meta), null);
                }
                writer.endWriteSequence();
            }
            return ToolResult.ofFile(output);
        } catch (IOException e) {
            throw new ToolProcessingException("GIF 생성 실패: " + e.getMessage(), e);
        }
    }

    private String resolveDisposal(String disposal) {
        return switch (disposal) {
            case "background" -> "restoreToBackgroundColor"; // 프레임마다 배경으로 지움 (일반 애니메이션)
            case "keep" -> "doNotDispose";                   // 이전 프레임 위에 겹침 (오버레이)
            case "previous" -> "restoreToPrevious";          // 이전 상태 복원 (스프라이트)
            default -> throw new ToolProcessingException(
                    "파라미터 'disposal'은 background/keep/previous 중 하나여야 합니다. (입력값: " + disposal + ")");
        };
    }

    /** frameWidth/frameHeight가 지정되면 프레임을 축소·확대한다 (종횡비 유지, 지정 크기 안에 맞춤). */
    private BufferedImage scaleFrame(BufferedImage frame, int width, int height) throws IOException {
        if (width <= 0 && height <= 0) return frame;
        if (width > 0 && height > 0) return Thumbnails.of(frame).size(width, height).asBufferedImage();
        if (width > 0) return Thumbnails.of(frame).width(width).asBufferedImage();
        return Thumbnails.of(frame).height(height).asBufferedImage();
    }

    private IIOMetadata buildFrameMetadata(ImageWriter writer, BufferedImage frame,
                                           int delayCs, int loopCount, String disposalMethod) throws IOException {
        IIOMetadata meta = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(frame), null);
        String format = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);

        // Graphic Control Extension — frame delay + disposal
        IIOMetadataNode gce = getOrCreate(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", disposalMethod);
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", String.valueOf(delayCs));
        gce.setAttribute("transparentColorIndex", "0");

        // Application Extension — Netscape loop (0 = 무한, N = N회 반복)
        IIOMetadataNode appExts = getOrCreate(root, "ApplicationExtensions");
        IIOMetadataNode appExt = new IIOMetadataNode("ApplicationExtension");
        appExt.setAttribute("applicationID", "NETSCAPE");
        appExt.setAttribute("authenticationCode", "2.0");
        appExt.setUserObject(new byte[]{
                0x1,
                (byte) (loopCount & 0xFF),        // little-endian short
                (byte) ((loopCount >> 8) & 0xFF),
        });
        appExts.appendChild(appExt);

        meta.setFromTree(format, root);
        return meta;
    }

    private IIOMetadataNode getOrCreate(IIOMetadataNode root, String nodeName) {
        NodeList nodes = root.getElementsByTagName(nodeName);
        if (nodes.getLength() > 0) return (IIOMetadataNode) nodes.item(0);
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }
}
