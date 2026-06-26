package com.metricslearn.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;


@SpringBootTest
@AutoConfigureMockMvc
public class ActuatorMetricsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void metricsIndexShouldBeAvailable() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.names").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.names[?(@ == 'jvm.memory.used')]").exists());
    }

    @Test
    void jvmMemoryUsedMetricShouldHaveMeasurements() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics/jvm.memory.used"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.measurements[0].value").isNumber());
    }

    @Test
    void httpServerRequestMetricShouldExistAfterApiCall() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/products/1"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics/http.server.requests"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("http.server.requests"));
    }
}
