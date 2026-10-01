package com.fincomp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.fincomp.config.BootstrapProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BootstrapProperties.class)
public class FincompApplication {

    public static void main(String[] args) {
        SpringApplication.run(FincompApplication.class, args);
    }
}
