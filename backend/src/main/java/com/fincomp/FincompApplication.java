package com.fincomp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FincompApplication {

    public static void main(String[] args) {
        SpringApplication.run(FincompApplication.class, args);
    }
}
