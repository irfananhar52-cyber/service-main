package com.irfan.email;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.irfan.email.EmailService;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @RabbitListener(queues = "order-queue",
    containerFactory = "rabbitListenerContainerFactory")
    public void receive(Order order) {
        try {
        System.out.println("MASUK CONSUMER==================: " + order);

        sendEmail(order);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEmail(Order order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("irfananhar52@gmail.com");
            message.setSubject("Order Berhasil Dibuat");
            message.setText(
                    "Halo,\n\n" +
                    "Order Anda berhasil dibuat.\n\n" +
                    "Order ID: " + order.getId() + "\n" +
                    "Produk: " + order.getProductName() + "\n\n" +
                    "=========================\n" +
                "Tugas Arsitektur Irfan.\n\n"
            );

            mailSender.send(message);
            System.out.println("Email terkirim");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}