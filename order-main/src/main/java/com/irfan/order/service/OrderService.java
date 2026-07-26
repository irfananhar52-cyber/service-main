package com.irfan.order.service;

import java.util.List;
import com.irfan.order.entity.Order;
import com.irfan.order.vo.ResponseTemplate;

public interface OrderService {

    Order create(Order order);

    List<Order> getAll();

    Order getById(Long id);

    Order update(Long id, Order order);

    void delete(Long id);
    List<ResponseTemplate> getOrderWithProdukById(Long id);
}