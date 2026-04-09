package com.travel.web;

import com.travel.common.exception.BusinessException;
import com.travel.common.exception.GlobalExceptionHandler;
import com.travel.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理 MVC 测试。
 * <p>
 * 覆盖权限异常与参数校验异常的标准返回结构。
 */
class GlobalExceptionHandlerMvcTest {

    private MockMvc mockMvc;

    /**
     * 仅挂载测试控制器和异常处理器，验证标准错误响应结构是否稳定。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessException_returnsUnifiedCodeAndMessage() throws Exception {
        mockMvc.perform(get("/api/test/exception/access-denied").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.ACCESS_DENIED.getMessage()));
    }

    @Test
    void runtimeException_returnsInternalServerErrorWithSystemErrorCode() throws Exception {
        mockMvc.perform(get("/api/test/exception/runtime").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ResultCode.SYSTEM_ERROR.getCode()));
    }

    @RestController
    static class TestExceptionController {

        @GetMapping("/api/test/exception/access-denied")
        public String throwAccessDenied() {
            throw new BusinessException(ResultCode.ACCESS_DENIED);
        }

        @GetMapping("/api/test/exception/runtime")
        public String throwRuntimeException() {
            throw new RuntimeException("unexpected");
        }
    }
}

