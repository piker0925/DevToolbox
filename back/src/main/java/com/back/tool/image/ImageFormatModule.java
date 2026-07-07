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
        try {
            String targetFormat = input.params().getOrDefault("targetFormat", "png").toLowerCase();
            if (targetFormat.equals("jpg")) targetFormat = "jpeg";
            String ext = targetFormat.equals("jpeg") ? "jpg" : targetFormat;
            Path src = input.files().get(0);
            Path output = Files.createTempFile("imgfmt-", "." + ext);
            Thumbnails.of(src.toFile())
                    .scale(1.0)
                    .outputFormat(targetFormat)
                    .toFile(output.toFile());
            return ToolResult.ofFile(output);
        } catch (IOException e) {
            throw new ToolProcessingException("이미지 포맷 변환 실패: " + e.getMessage(), e);
        }
    }
}
