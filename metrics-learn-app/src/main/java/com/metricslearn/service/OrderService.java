package com.metricslearn.service;

import com.metricslearn.domain.Order;
import com.metricslearn.domain.Product;
import com.metricslearn.repository.OrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class OrderService {
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final MeterRegistry meterRegistry;

    public OrderService(ProductService productService, OrderRepository orderRepository, MeterRegistry meterRegistry) {
        this.productService = productService;
        this.orderRepository = orderRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Optional<Order> createOrder(Long productId, Integer quantity) {
        Optional<Product> productOpt = productService.findById(productId);
        if (productOpt.isEmpty()) {
            meterRegistry.counter("app.orders.total", "status","failure").increment();
            return Optional.empty();
        }

        Product product = productOpt.get();
        double amount = product.price() * quantity;
        Order order = new Order(productId, quantity, amount, Instant.now());
        Order saved = orderRepository.save(order);
        meterRegistry.counter("app.orders.total", "status", "success").increment();
        return Optional.of(saved);

    }

    @Transactional
    public Optional<Order> findById(Long id){
        return Timer.builder("app.orders.query")
                .description("Order query duration")
                .register(meterRegistry)
                .record(()->orderRepository.findById(id));
    }
}
