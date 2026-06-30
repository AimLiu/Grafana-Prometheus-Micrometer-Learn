package com.metricslearn.actuator;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;



@SpringBootTest
@AutoConfigureMockMvc
class ActuatorPrometheusEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prometheusEndpointShouldReturnPlainTextMetrics() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/products/1"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.allOf(
                        Matchers.containsString("# HELP jvm_memory_used_bytes"),
                        Matchers.containsString("# TYPE jvm_memory_used_bytes gauge"),
                        Matchers.containsString("# TYPE http_server_requests_seconds"),
                        Matchers.containsString("http_server_requests_seconds_count"),
                        Matchers.containsString("application=\"metrics-learn\""),
                        Matchers.containsString("environment=\"local\"")
                )));
    }

    @Test
    void prometheusEndpointShouldContainHttpUriLabelAfterApiCall() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/products/2"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("uri=\"/api/products/{id}\"")));
    }
}