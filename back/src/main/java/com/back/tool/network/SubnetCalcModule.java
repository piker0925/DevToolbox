package com.back.tool.network;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.springframework.stereotype.Component;

@Component
public class SubnetCalcModule implements ToolModule {

    @Override
    public String getId() { return "subnet-calc"; }

    @Override
    public String getName() { return "서브넷 계산기"; }

    @Override
    public String getCategory() { return "network"; }

    @Override
    public boolean isHeavy() { return false; }

    @Override
    public ToolResult process(ToolInput input) {
        String cidr = input.params().getOrDefault("cidr", "");
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) throw new IllegalArgumentException("CIDR 형식이 아닙니다: " + cidr);

            long ip = ipToLong(parts[0]);
            int prefix = Integer.parseInt(parts[1].trim());
            if (prefix < 0 || prefix > 32) throw new IllegalArgumentException("prefix 범위 오류: " + prefix);

            long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            long network = ip & mask;
            long broadcast = network | (~mask & 0xFFFFFFFFL);
            long hosts = prefix >= 31 ? 0 : broadcast - network - 1;

            return ToolResult.ofText(String.join("\n",
                    "네트워크 주소:  " + longToIp(network),
                    "브로드캐스트:   " + longToIp(broadcast),
                    "서브넷 마스크:  " + longToIp(mask),
                    "첫 호스트:      " + longToIp(network + 1),
                    "마지막 호스트:  " + longToIp(broadcast - 1),
                    "사용 가능 호스트: " + hosts
            ));
        } catch (IllegalArgumentException e) {
            throw new ToolProcessingException(e.getMessage(), e);
        }
    }

    private long ipToLong(String ip) {
        String[] octs = ip.trim().split("\\.");
        if (octs.length != 4) throw new IllegalArgumentException("잘못된 IP 주소: " + ip);
        long result = 0;
        for (String oct : octs) {
            int val = Integer.parseInt(oct);
            if (val < 0 || val > 255) throw new IllegalArgumentException("옥텟 범위 오류: " + val);
            result = (result << 8) | val;
        }
        return result;
    }

    private String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
    }
}
