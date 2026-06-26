package com.metricslearn.service;


import com.metricslearn.domain.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private final Map<Long, Product> products = Map.of(
            1L, new Product(1L, "Prometheus 入门手册", 49.90),
            2L, new Product(2L, "Grafana 仪表盘实战", 59.90),
            3L, new Product(3L, "Micrometer 埋点指南", 39.90)
    );

    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }
}