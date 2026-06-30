package com.metricslearn.controller;

import com.metricslearn.service.CacheDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CacheDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CacheDemoService cacheDemoService;

    @Test
    void getCacheShouldReturn200WhenFound() throws Exception {
        when(cacheDemoService.get("k1")).thenReturn(Optional.of("v1"));

        mockMvc.perform(get("/api/cache/demo/k1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("k1"))
                .andExpect(jsonPath("$.value").value("v1"));
    }

    @Test
    void putCacheShouldReturn204() throws Exception {
        mockMvc.perform(post("/api/cache/demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"k2\",\"value\":\"v2\"}"))
                .andExpect(status().isNoContent());

        verify(cacheDemoService).put("k2", "v2");
    }
}