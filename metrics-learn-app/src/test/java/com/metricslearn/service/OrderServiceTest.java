package com.metricslearn.service;

import com.metricslearn.domain.Product;
import com.metricslearn.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.metricslearn.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private ProductService productService;

    @Mock
    private OrderRepository orderRepository;

    private MeterRegistry meterRegistry;
    private OrderService orderService;

    @BeforeEach
    void setUp(){
        meterRegistry = new SimpleMeterRegistry();
        orderService = new OrderService(productService, orderRepository, meterRegistry);
    }


    @Test
    void creatOrderShouldIncrementSuccessCounter(){
        when(productService.findById(1L))
                .thenReturn(Optional.of(new Product(1L, "Promethues 入门手册", 49.00)));
        when(orderRepository.save(Mockito.any(Order.class)))
                .thenAnswer(inv -> {
                    Order obj = inv.getArgument(0);
                    return new Order(obj.getProductId(), obj.getQuantity(), obj.getAmount(), obj.getCreatedAt());
                });
        orderService.createOrder(1L, 2);

        Counter counter = meterRegistry.find("app.orders.total").tag("status", "success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void createOrderShouldIncrementFailureCounterWhenProductMissing(){
        when(productService.findById(999L)).thenReturn(Optional.empty());
        orderService.createOrder(999L, 1);

        Counter counter = meterRegistry.find("app.orders.total").tag("status", "failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

    }
}
