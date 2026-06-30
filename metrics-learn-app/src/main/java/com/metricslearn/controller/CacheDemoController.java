package com.metricslearn.controller;

import com.metricslearn.service.CacheDemoService;
import com.metricslearn.dto.CacheWriteRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cache/demo")
public class CacheDemoController {

    private final CacheDemoService cacheDemoService;

    public CacheDemoController(CacheDemoService cacheDemoService) {
        this.cacheDemoService = cacheDemoService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> get(@PathVariable String key) {
        return cacheDemoService.get(key)
                .map(value -> ResponseEntity.ok(Map.of("key", key, "value", value)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> put(@RequestBody CacheWriteRequest request) {
        cacheDemoService.put(request.key(), request.value());
        return ResponseEntity.noContent().build();
    }
}