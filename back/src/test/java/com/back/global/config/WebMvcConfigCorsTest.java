package com.back.global.config;

import com.back.AbstractMySQLIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 브라우저는 PATCH처럼 "단순 요청"이 아닌 메서드를 보내기 전에 CORS preflight(OPTIONS)를 먼저 보내고,
// Access-Control-Allow-Methods에 그 메서드가 없으면 실제 요청 자체를 안 보낸다. MockMvc로 컨트롤러를
// 직접 두드리거나 curl로 PATCH를 바로 보내는 테스트는 이 단계를 거치지 않아 놓치기 쉽다 — 실제로
// PATCH /api/v1/users/me(닉네임 수정)가 CORS 허용 목록에서 빠져 있었는데 이런 종류의 테스트가 없어
// 못 잡았었다.
@SpringBootTest
class WebMvcConfigCorsTest extends AbstractMySQLIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @Test
    void PATCH_users_me는_CORS_preflight를_통과한다() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();

        mockMvc.perform(options("/api/v1/users/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));
    }
}
