package com.cool.app.impl;

import com.cool.api.dto.InventoryResponse;
import com.cool.app.OrderAppService;
import com.cool.domain.event.OrderEvent;
import com.cool.domain.model.Order;
import com.cool.domain.model.OrderLineItem;
import com.cool.domain.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class OrderAppServiceImpl implements OrderAppService {

    @Autowired
    OrderService orderService;

    @Autowired
    WebClient.Builder webClientBuilder;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    @CircuitBreaker(name = "placeOrder", fallbackMethod = "placeOrderFallback")
    public String placeOrder(Order order) {
        log.info("start place order{}", order.getOrderNo());

        List<String> skuCodes = order.getOrderLineItems().stream().map(OrderLineItem::getSkuCode).toList();

        for (String skuCode : skuCodes) {
            boolean isProductExist = webClientBuilder.build().get().uri("http://localhost:8084",
                    uriBuilder -> uriBuilder.path("/api/product/exist/{skuCode}")
                            .build(skuCode)).retrieve().bodyToMono(Boolean.class).block();
            if (!isProductExist) {
                return "No such product";
            }
        }

        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://localhost:9082/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCodes", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

        if (allProductsInStock) {
            log.info("end place order{}", order.getOrderNo());
            Order order1 = orderService.placeOrder(order);
            rabbitTemplate.convertAndSend("order-queue", OrderEvent.builder().orderNo(order1.getOrderNo()).build());
            return "Order Placed Successfully";
        } else {
            return "Product is not in stock, please try again later";
        }
    }

    public String placeOrderFallback(Order order, RuntimeException e) {
        return "Oops!, something went wrong, please retry later!" + System.lineSeparator() + e.getMessage();
    }
}
