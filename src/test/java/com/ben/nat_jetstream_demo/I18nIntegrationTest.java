package com.ben.nat_jetstream_demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class I18nIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testMissingParameterInEnglish() throws Exception {
        // ChaosController.enableFailure expects 'keyword' parameter
        mockMvc.perform(post("/api/chaos/enable")
                        .header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: keyword"));
    }

    @Test
    public void testMissingParameterInChinese() throws Exception {
        // 我們預期會拿到 messages_zh_TW.yml 中的內容
        mockMvc.perform(post("/api/chaos/enable")
                        .header("Accept-Language", "zh-TW"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少必要的參數: keyword"));
    }

    @Test
    public void testInternalErrorInEnglish() throws Exception {
        // 觸發一個傳入負數的情況，Controller 會拋出 AppException("error.internal_test")
        // 預期回傳 500 Internal Server Error 以及對應的英文訊息
        mockMvc.perform(post("/api/chaos/max-failures")
                        .param("max", "-1")
                        .header("Accept-Language", "en"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An internal server error occurred for test purposes."));
    }
}
