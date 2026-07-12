package com.back.tool.generator;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeModuleTest {

    private final QrCodeModule module = new QrCodeModule();

    private static BufferedImage render(ToolResult result) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(result.textResult());
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(img).isNotNull();
        return img;
    }

    private static Result decodeFull(BufferedImage img) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        return new MultiFormatReader().decode(bitmap);
    }

    private static String decode(BufferedImage img) throws Exception {
        return decodeFull(img).getText();
    }

    /** 이미지 가운데 행에서 왼쪽 가장자리부터 연속된 배경색 픽셀 길이(quiet zone 폭)를 잰다. */
    private static int quietZoneRun(BufferedImage img, int backgroundRgb) {
        int y = img.getHeight() / 2;
        int run = 0;
        while (run < img.getWidth() && (img.getRGB(run, y) & 0xFFFFFF) == backgroundRgb) {
            run++;
        }
        return run;
    }

    @Test
    void generatesValidBase64Png() throws Exception {
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("content", "https://example.com")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).isNotBlank();

        BufferedImage img = render(result);
        assertThat(img.getWidth()).isGreaterThan(0);
        assertThat(decode(img)).isEqualTo("https://example.com");
    }

    @Test
    void customSize() throws Exception {
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("content", "hello", "size", "200")
        ));

        BufferedImage img = render(result);
        assertThat(img.getWidth()).isEqualTo(200);
        assertThat(img.getHeight()).isEqualTo(200);
        assertThat(decode(img)).isEqualTo("hello");
    }

    @Test
    void errorCorrectionLevelIsAppliedToEncodedQr() throws Exception {
        // 패턴 B: L과 H 두 시나리오 — 디코드 메타데이터로 실제 적용 여부를 구분한다.
        BufferedImage low = render(module.process(new ToolInput(
                List.of(), Map.of("content", "hello", "errorCorrection", "L")
        )));
        BufferedImage high = render(module.process(new ToolInput(
                List.of(), Map.of("content", "hello", "errorCorrection", "H")
        )));

        assertThat(decodeFull(low).getResultMetadata()
                .get(ResultMetadataType.ERROR_CORRECTION_LEVEL)).isEqualTo("L");
        assertThat(decodeFull(high).getResultMetadata()
                .get(ResultMetadataType.ERROR_CORRECTION_LEVEL)).isEqualTo("H");
    }

    @Test
    void defaultErrorCorrectionIsM() throws Exception {
        BufferedImage img = render(module.process(new ToolInput(
                List.of(), Map.of("content", "hello")
        )));
        assertThat(decodeFull(img).getResultMetadata()
                .get(ResultMetadataType.ERROR_CORRECTION_LEVEL)).isEqualTo("M");
    }

    @Test
    void customColorsAreRendered() throws Exception {
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of(
                "content", "colored",
                "foreground", "#112233",
                "background", "#EEDDCC"
        )));
        BufferedImage img = render(result);

        // 기본 margin=4 > 0 이므로 좌상단 픽셀은 배경색이어야 한다
        assertThat(img.getRGB(0, 0) & 0xFFFFFF).isEqualTo(0xEEDDCC);

        boolean hasForeground = false;
        for (int y = 0; y < img.getHeight() && !hasForeground; y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) & 0xFFFFFF) == 0x112233) {
                    hasForeground = true;
                    break;
                }
            }
        }
        assertThat(hasForeground).as("전경색 #112233 픽셀 존재").isTrue();

        // 색상이 바뀌어도 스캔 가능해야 한다
        assertThat(decode(img)).isEqualTo("colored");
    }

    @Test
    void marginControlsQuietZoneWidth() throws Exception {
        // 패턴 B: margin=0 과 margin=8 을 비교 — 값이 실제 반영되는지 구분한다.
        BufferedImage noMargin = render(module.process(new ToolInput(
                List.of(), Map.of("content", "hello", "margin", "0")
        )));
        BufferedImage wideMargin = render(module.process(new ToolInput(
                List.of(), Map.of("content", "hello", "margin", "8")
        )));

        int runNone = quietZoneRun(noMargin, 0xFFFFFF);
        int runWide = quietZoneRun(wideMargin, 0xFFFFFF);
        assertThat(runNone).isLessThan(runWide);
        // margin=8 이면 최소 8모듈 이상의 quiet zone이 생긴다
        assertThat(runWide).isGreaterThanOrEqualTo(8);
    }

    @Test
    void rejectsInvalidHexColor() {
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "hello", "background", "red")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("background")
                .hasMessageContaining("#RRGGBB");
    }

    @Test
    void rejectsSizeOutOfRange() {
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "hello", "size", "10000")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("size");
    }

    @Test
    void rejectsMissingContent() {
        assertThatThrownBy(() -> module.process(new ToolInput(List.of(), Map.of())))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("content");
    }

    @Test
    void moduleMetadata() {
        assertThat(module.getId()).isEqualTo("qr-code");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("generator");
    }
}
