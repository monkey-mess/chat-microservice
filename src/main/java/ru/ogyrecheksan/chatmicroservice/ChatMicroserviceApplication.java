package ru.ogyrecheksan.chatmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ChatMicroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatMicroserviceApplication.class, args);
    }

}
