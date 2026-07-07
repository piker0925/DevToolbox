package com.back.tool.image;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ImageResizeModule implements ToolModule {

    @Override
    public String getId() { return "image-resize"; }

    @Override
    public String getName() { return "이미지 리사이즈"; }

    @Override
    public String getCategory() { return "image"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        try {
            int width = Integer.parseInt(input.params().getOrDefault("width", "800"));
            int height = Integer.parseInt(input.params().getOrDefault("height", "600"));
            Path src = input.files().get(0);
            String ext = extension(src);
            Path output = Files.createTempFile("resize-", "." + ext);
            Thumbnails.of(src.toFile())
                    .size(width, height)
                    .keepAspectRatio(false)
                    .toFile(output.toFile());
            return ToolResult.ofFile(output);
        } catch (IOException e) {
            throw new ToolProcessingException("이미지 리사이즈 실패: " + e.getMessage(), e);
        }
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "png";
    }
}
