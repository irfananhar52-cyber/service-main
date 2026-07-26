package com.irfan.demo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);
    private static final int MAX_MESSAGES = 100;

    private final CopyOnWriteArrayList<String> receivedMessages = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void consumeMessage(String message) {
        String formatted = LocalDateTime.now() + " - " + message;
        receivedMessages.add(formatted);

        // Keep only the latest messages to avoid unbounded memory growth.
        if (receivedMessages.size() > MAX_MESSAGES) {
            receivedMessages.remove(0);
        }

        logger.info("Message received successfully: {}", message);
    }

    public List<String> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }
}
