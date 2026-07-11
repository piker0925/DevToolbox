package com.back.tool.network;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubnetCalcModuleTest {

    @Test
    void calculatesSlash24Subnet() {
        SubnetCalcModule module = new SubnetCalcModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("cidr", "192.168.1.0/24")
        ));

        assertThat(result.isFile()).isFalse();
        // 라벨과 값을 함께 검증 — "254"가 마지막 호스트 줄(192.168.1.254)에서 우연히 통과하던
        // 문제를 막고, 각 계산값이 올바른 필드에 붙었는지 확인한다.
        assertThat(result.textResult()).contains("네트워크 주소:  192.168.1.0");
        assertThat(result.textResult()).contains("브로드캐스트:   192.168.1.255");
        assertThat(result.textResult()).contains("서브넷 마스크:  255.255.255.0");
        assertThat(result.textResult()).contains("첫 호스트:      192.168.1.1");
        assertThat(result.textResult()).contains("마지막 호스트:  192.168.1.254");
        assertThat(result.textResult()).contains("사용 가능 호스트: 254");
    }

    @Test
    void calculatesSlash16Subnet() {
        SubnetCalcModule module = new SubnetCalcModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("cidr", "10.0.0.0/16")
        ));

        assertThat(result.textResult()).contains("네트워크 주소:  10.0.0.0");
        assertThat(result.textResult()).contains("브로드캐스트:   10.0.255.255");
        assertThat(result.textResult()).contains("서브넷 마스크:  255.255.0.0");
        assertThat(result.textResult()).contains("첫 호스트:      10.0.0.1");
        assertThat(result.textResult()).contains("마지막 호스트:  10.0.255.254");
        assertThat(result.textResult()).contains("사용 가능 호스트: 65534");
    }

    @Test
    void slash31HasZeroUsableHosts() {
        SubnetCalcModule module = new SubnetCalcModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("cidr", "192.168.1.0/31")
        ));

        // 엣지 케이스: /31은 사용 가능 호스트가 0이어야 한다 (broadcast-network-1 공식을 그대로 쓰면 음수).
        assertThat(result.textResult()).contains("서브넷 마스크:  255.255.255.254");
        assertThat(result.textResult()).contains("사용 가능 호스트: 0");
    }

    @Test
    void moduleMetadata() {
        SubnetCalcModule module = new SubnetCalcModule();
        assertThat(module.getId()).isEqualTo("subnet-calc");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("network");
    }
}
