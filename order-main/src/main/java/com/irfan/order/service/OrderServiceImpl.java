package com.irfan.order.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.RestTemplate;
import com.irfan.order.entity.Order;
import com.irfan.order.repository.OrderRepository;
import com.irfan.order.vo.ResponseTemplate;
import com.irfan.order.vo.Product;
import java.util.ArrayList;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderRepository repository;
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    public OrderServiceImpl(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Order create(Order order) {
        logger.info("Saving order for customerName={} productId={}", order.getCustomerName(), order.getProductId());
        Order savedOrder = repository.save(order);
        logger.info("Order saved with id={}", savedOrder.getId());
        rabbitTemplate.convertAndSend("order-queue", savedOrder);

        logger.info("Order message published to queue order-queue with id={}", savedOrder.getId());

        return savedOrder;
        
    }

    @Override
    public List<Order> getAll() {
        return repository.findAll();
    }

    @Override
    public Order getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Order update(Long id, Order order) {

        Order existing = repository.findById(id).orElse(null);

        if (existing != null) {
            existing.setCustomerName(order.getCustomerName());
            existing.setProductName(order.getProductName());
            existing.setQuantity(order.getQuantity());
            existing.setPrice(order.getPrice());
            existing.setProductId(order.getProductId());

            return repository.save(existing);
        }

        return null;
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<ResponseTemplate> getOrderWithProdukById(Long id) {

        List<ResponseTemplate> responseList = new ArrayList<>();

        // ambil order
        Order order = getById(id);

        // ambil service PRODUK dari Eureka
        List<ServiceInstance> instances = discoveryClient.getInstances("PRODUCT");

        if (instances.isEmpty()) {
            logger.error("Product service not found in Eureka for orderId={}", id);
            throw new RuntimeException("Service PRODUK tidak ditemukan di Eureka");
        }

        ServiceInstance serviceInstance = instances.get(0);

        // bentuk URL
        String url = serviceInstance.getUri().toString() + "/products/" + order.getProductId();

        logger.info("Calling product service url={} for orderId={}", url, id);

        // Forward JWT if present
        HttpHeaders headers = new HttpHeaders();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null) {
            headers.set("Authorization", "Bearer " + auth.getCredentials().toString());
        } else {
            logger.warn("No JWT token found in SecurityContext for orderId={}", id);
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // call API
        Product product;
        try {
            product = restTemplate.exchange(url, HttpMethod.GET, entity, Product.class).getBody();
        } catch (RestClientException e) {
            logger.error("Failed to fetch product data for orderId={} message={}", id, e.getMessage());
            throw new RuntimeException("Gagal mengambil data product: " + e.getMessage());
        }

        if (product == null) {
            logger.error("Product response was empty for orderId={}", id);
            throw new RuntimeException("Produk tidak ditemukan");
        }

        // mapping response
        ResponseTemplate vo = new ResponseTemplate();
        vo.setOrder(order);
        vo.setProduct(product);

        responseList.add(vo);

        return responseList;
    }
}