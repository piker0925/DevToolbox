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
        assertThat(result.textResult()).contains("192.168.1.0");      // network
        assertThat(result.textResult()).contains("192.168.1.255");    // broadcast
        assertThat(result.textResult()).contains("255.255.255.0");    // mask
        assertThat(result.textResult()).contains("254");              // usable hosts
    }

    @Test
    void calculatesSlash16Subnet() {
        SubnetCalcModule module = new SubnetCalcModule();
        ToolResult result = module.process(new ToolInput(
                List.of(), Map.of("cidr", "10.0.0.0/16")
        ));

        assertThat(result.textResult()).contains("10.0.0.0");
        assertThat(result.textResult()).contains("10.0.255.255");
        assertThat(result.textResult()).contains("255.255.0.0");
    }

    @Test
    void moduleMetadata() {
        SubnetCalcModule module = new SubnetCalcModule();
        assertThat(module.getId()).isEqualTo("subnet-calc");
        assertThat(module.isHeavy()).isFalse();
        assertThat(module.getCategory()).isEqualTo("network");
    }
}
