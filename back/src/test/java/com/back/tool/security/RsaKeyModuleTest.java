package com.back.tool.security;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RsaKeyModuleTest {

    /**
     * PEM의 PUBLIC KEY 블록을 지정한 알고리즘으로 파싱한다.
     * 요청한 keyType과 실제 키 알고리즘이 다르면 InvalidKeySpecException이 나서 테스트가 실패한다
     * (예: EC를 요청했는데 RSA 키가 나오면 여기서 걸린다).
     */
    private static PublicKey publicKey(String pem, String algorithm) throws Exception {
        String base64 = pem
                .replaceAll("(?s).*-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("(?s)-----END PUBLIC KEY-----.*", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(der));
    }

    @Test
    void generatesRsa2048KeyPairInPem() throws Exception {
        RsaKeyModule module = new RsaKeyModule();
        ToolResult result = module.process(new ToolInput(List.of(), Map.of("keyType", "RSA", "keySize", "2048")));

        assertThat(result.isFile()).isFalse();
        assertThat(result.textResult()).contains("-----BEGIN PUBLIC KEY-----");
        assertThat(result.textResult()).contains("-----BEGIN PRIVATE KEY-----");

        RSAPublicKey pub = (RSAPublicKey) publicKey(result.textResult(), "RSA");
        assertThat(pub.getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void generatesEcKeyPair() throws Exception {
        RsaKeyModule module = new RsaKeyModule();
        ToolResult result = module.process(new ToolInput(List.of(), Map.of("keyType", "EC", "keySize", "256")));

        assertThat(result.textResult()).contains("-----BEGIN PUBLIC KEY-----");
        assertThat(result.textResult()).contains("-----BEGIN PRIVATE KEY-----");

        // EC를 요청했는데 기본 RSA로 고정 반환하는 뮤턴트는 EC 파싱에서 실패한다.
        ECPublicKey pub = (ECPublicKey) publicKey(result.textResult(), "EC");
        assertThat(pub.getParams().getCurve().getField().getFieldSize()).isEqualTo(256);
    }

    @Test
    void keySizeParamSelectsCurve() throws Exception {
        RsaKeyModule module = new RsaKeyModule();
        ToolResult result = module.process(new ToolInput(List.of(), Map.of("keyType", "EC", "keySize", "384")));

        // keySize를 무시하고 256으로 고정하는 뮤턴트를 잡는다.
        ECPublicKey pub = (ECPublicKey) publicKey(result.textResult(), "EC");
        assertThat(pub.getParams().getCurve().getField().getFieldSize()).isEqualTo(384);
    }

    @Test
    void moduleMetadata() {
        RsaKeyModule module = new RsaKeyModule();
        assertThat(module.getId()).isEqualTo("rsa-key");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("security");
    }
}
