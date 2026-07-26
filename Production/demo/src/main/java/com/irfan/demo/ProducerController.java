package com.irfan.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProducerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    @Autowired
    private ProducerService producerService;

    @Autowired
    private ConsumerService consumerService;

    @GetMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestParam(required = true) String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message parameter is required and cannot be empty");
            }
            producerService.sendMessage(message);
            logger.info("Message endpoint called with: " + message);
            return ResponseEntity.ok("Message sent successfully: " + message);
        } catch (Exception e) {
            logger.error("Error in sendMessage endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application is running");
    }

    @GetMapping("/received")
    public ResponseEntity<Object> receivedMessages() {
        return ResponseEntity.ok(consumerService.getReceivedMessages());
    }
}
