package com.back.global.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 클라이언트 IP 추출(040). nginx가 X-Real-IP를 $remote_addr로 덮어쓰므로(스푸핑 불가) 이를 우선
 * 신뢰하고, 없으면(로컬 개발·직접 접속) RemoteAddr로 폴백한다.
 * X-Forwarded-For는 첫 값이 클라이언트가 임의로 넣을 수 있어(스푸핑 가능) 신뢰하지 않는다.
 */
class ClientIpResolverTest {

    @Test
    void X_Real_IP_헤더가_있으면_그_값을_사용한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.5");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void X_Real_IP_헤더가_없으면_RemoteAddr로_폴백한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void X_Real_IP_헤더가_빈_문자열이면_RemoteAddr로_폴백한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("127.0.0.1");
    }
}
