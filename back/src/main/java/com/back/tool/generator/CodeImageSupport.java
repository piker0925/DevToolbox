package com.back.tool.generator;

import com.back.tool.model.ToolProcessingException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * QR/바코드 모듈 공용 이미지 렌더링 헬퍼.
 * hex 색상 파싱과 BitMatrix → Base64 PNG 변환을 담당한다.
 */
final class CodeImageSupport {

    private CodeImageSupport() {
    }

    /**
     * "#RRGGBB" 또는 "RRGGBB" 형식의 hex 색상을 ARGB int로 변환한다.
     * 형식이 잘못되면 한국어 메시지의 {@link ToolProcessingException}을 던진다.
     */
    static int parseHexColor(String hex, String paramName) {
        String v = hex.trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        if (!v.matches("[0-9a-fA-F]{6}")) {
            throw new ToolProcessingException(
                    "파라미터 '" + paramName + "'는 #RRGGBB 형식의 hex 색상이어야 합니다. (입력값: " + hex + ")");
        }
        return 0xFF000000 | Integer.parseInt(v, 16);
    }

    static String toBase64Png(BitMatrix matrix, int onColor, int offColor) throws IOException {
        BufferedImage img = MatrixToImageWriter.toBufferedImage(
                matrix, new MatrixToImageConfig(onColor, offColor));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
