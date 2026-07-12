package com.back.tool.generator;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
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

class BarcodeModuleTest {

    private final BarcodeModule module = new BarcodeModule();

    private static BufferedImage render(ToolResult result) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(result.textResult());
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(img).isNotNull();
        return img;
    }

    private static Result decode(BufferedImage img) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        return new MultiFormatReader().decode(bitmap);
    }

    @Test
    void generatesValidBase64Png() throws Exception {
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("content", "1234567890")
        ));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).isNotBlank();

        BufferedImage img = render(result);
        assertThat(img.getWidth()).isGreaterThan(0);

        Result decoded = decode(img);
        assertThat(decoded.getText()).isEqualTo("1234567890");
    }

    @Test
    void defaultFormatIsCode128() throws Exception {
        // 패턴 B: format 미지정(기본값)과 ean13 지정 두 시나리오로 실제 형식 적용을 구분한다.
        BufferedImage img = render(module.process(new ToolInput(
                List.of(), Map.of("content", "ABC-1234")
        )));
        Result decoded = decode(img);
        assertThat(decoded.getBarcodeFormat()).isEqualTo(BarcodeFormat.CODE_128);
        assertThat(decoded.getText()).isEqualTo("ABC-1234");
    }

    @Test
    void ean13With13ValidDigitsRoundTrips() throws Exception {
        // 4006381333931 은 체크 디지트가 유효한 공지된 EAN-13 예시 값
        BufferedImage img = render(module.process(new ToolInput(
                List.of(), Map.of("content", "4006381333931", "format", "ean13")
        )));
        Result decoded = decode(img);
        assertThat(decoded.getBarcodeFormat()).isEqualTo(BarcodeFormat.EAN_13);
        assertThat(decoded.getText()).isEqualTo("4006381333931");
    }

    @Test
    void ean13With12DigitsAppendsCheckDigit() throws Exception {
        // 앞 12자리 400638133393 의 체크 디지트는 1 — 독립 기준값과 비교
        BufferedImage img = render(module.process(new ToolInput(
                List.of(), Map.of("content", "400638133393", "format", "ean13")
        )));
        Result decoded = decode(img);
        assertThat(decoded.getBarcodeFormat()).isEqualTo(BarcodeFormat.EAN_13);
        assertThat(decoded.getText()).isEqualTo("4006381333931");
    }

    @Test
    void ean13RejectsInvalidCheckDigit() {
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "4006381333930", "format", "ean13")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("체크 디지트")
                .hasMessageContaining("1");
    }

    @Test
    void ean13RejectsNonNumericOrWrongLength() {
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "abc", "format", "ean13")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("12~13자리");

        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "12345", "format", "ean13")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("12~13자리");
    }

    @Test
    void code128RejectsNonAsciiContent() {
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "한글바코드")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("ASCII");
    }

    @Test
    void rejectsUnknownFormat() {
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "1234", "format", "qr")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("format");
    }

    @Test
    void customColorsAreRendered() throws Exception {
        BufferedImage img = render(module.process(new ToolInput(
                List.of(), Map.of(
                "content", "COLOR-1",
                "foreground", "#112233",
                "background", "#EEDDCC"
        ))));

        // 바코드 상단 모서리는 quiet zone(배경색)이어야 한다
        assertThat(img.getRGB(0, 0) & 0xFFFFFF).isEqualTo(0xEEDDCC);

        boolean hasForeground = false;
        int y = img.getHeight() / 2;
        for (int x = 0; x < img.getWidth(); x++) {
            if ((img.getRGB(x, y) & 0xFFFFFF) == 0x112233) {
                hasForeground = true;
                break;
            }
        }
        assertThat(hasForeground).as("전경색 #112233 픽셀 존재").isTrue();

        // 색상이 바뀌어도 스캔 가능해야 한다
        assertThat(decode(img).getText()).isEqualTo("COLOR-1");
    }

    @Test
    void rejectsInvalidHexColor() {
        assertThatThrownBy(() -> module.process(new ToolInput(
                List.of(), Map.of("content", "1234", "foreground", "#12")
        )))
                .isInstanceOf(ToolProcessingException.class)
                .hasMessageContaining("foreground")
                .hasMessageContaining("#RRGGBB");
    }

    @Test
    void ean13CheckDigitMatchesKnownReference() {
        // 독립 기준값: 4006381333931(공지된 예시), 8801234567893(수기 계산)
        assertThat(BarcodeModule.ean13CheckDigit("400638133393")).isEqualTo(1);
        assertThat(BarcodeModule.ean13CheckDigit("880123456789")).isEqualTo(3);
    }

    @Test
    void moduleMetadata() {
        assertThat(module.getId()).isEqualTo("barcode");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("generator");
    }
}
