package com.irfan.cunsumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStarter implements CommandLineRunner {

    @Autowired
    private MessageProducer producer;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========================================");
        System.out.println("✓ Aplikasi dimulai");
        System.out.println("========================================\n");

        // Mengirim beberapa pesan untuk demo
        producer.sendMessage("Halo dari Consumer App 1");
        Thread.sleep(1000);
        
        producer.sendMessage("Halo dari Consumer App 2");
        Thread.sleep(1000);
        
        producer.sendMessage("Halo dari Consumer App 3");
        
        System.out.println("\n========================================");
        System.out.println("✓ Aplikasi siap mendengarkan pesan");
        System.out.println("========================================\n");
    }
}
