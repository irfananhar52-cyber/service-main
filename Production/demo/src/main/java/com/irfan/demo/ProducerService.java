package com.irfan.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProducerService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public void sendMessage(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Message cannot be null or empty");
            }
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            logger.info("Message sent successfully: " + message);
        } catch (Exception e) {
            logger.error("Error sending message: " + message, e);
            throw new RuntimeException("Failed to send message", e);
        }
    }
}