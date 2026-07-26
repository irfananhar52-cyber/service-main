package com.irfan.order.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.irfan.order.entity.Order;
import com.irfan.order.service.OrderService;
import com.irfan.order.vo.ResponseTemplate;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService service;

    @PostMapping
    public Order create(@RequestBody Order order) {
        logger.info("Create order request received for productId={} customerName={}", order.getProductId(), order.getCustomerName());
        return service.create(order);
    }

    @GetMapping
    public List<Order> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        logger.info("Fetch order by id={}", id);
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable Long id, @RequestBody Order order) {
        logger.info("Update order request for id={}", id);
        return service.update(id, order);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        logger.info("Delete order request for id={}", id);
        service.delete(id);
        return "Order deleted";
    }

    @GetMapping("/products/{id}")
    public List<ResponseTemplate> getOrderWithProdukById(@PathVariable Long id) {
        logger.info("Fetch order with product details for id={}", id);
        return service.getOrderWithProdukById(id);
    }
}