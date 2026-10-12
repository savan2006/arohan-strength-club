package com.arohan.strengthclub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArohanStrengthClubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArohanStrengthClubApplication.class, args);
    }
}
