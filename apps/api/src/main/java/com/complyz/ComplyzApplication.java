package com.complyz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.complyz.config.BootstrapProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BootstrapProperties.class)
public class ComplyzApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplyzApplication.class, args);
    }
}
