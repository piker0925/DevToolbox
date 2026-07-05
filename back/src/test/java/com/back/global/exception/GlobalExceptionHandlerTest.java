package com.back.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new FakeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @RestController
    static class FakeController {

        @GetMapping("/test/app-exception")
        String throwAppException() {
            throw new AppException(ErrorCode.MODULE_NOT_FOUND);
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody ValidRequest request) {
            return "ok";
        }

        record ValidRequest(@NotBlank String name) {}
    }

    @Test
    void appException_returnsErrorResponse() throws Exception {
        mockMvc.perform(get("/test/app-exception"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("MODULE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("존재하지 않는 도구입니다."))
            .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void validationFailure_returnsFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void unknownUrl_returns404ErrorResponse() throws Exception {
        mockMvc.perform(get("/nonexistent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
