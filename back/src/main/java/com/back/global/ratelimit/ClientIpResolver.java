package com.back.global.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 클라이언트 IP 추출(040). nginx가 X-Real-IP를 $remote_addr로 set(덮어쓰기)하므로 클라이언트가
 * 조작할 수 없다 — 이를 우선 신뢰한다. X-Forwarded-For는 nginx가 append만 하기 때문에 클라이언트가
 * 보낸 첫 값이 그대로 남아 스푸핑 가능해 사용하지 않는다. 헤더가 없으면(로컬 개발·nginx를 거치지
 * 않은 직접 접속) RemoteAddr로 폴백한다.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
