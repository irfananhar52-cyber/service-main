package com.irfan.cunsumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MessageListener {

    @RabbitListener(queues = "myQueue")
    public void receiveMessage(String message) {
        System.out.println("✓ Pesan diterima: " + message);
        System.out.println("Waktu: " + java.time.LocalDateTime.now());
    }
}
