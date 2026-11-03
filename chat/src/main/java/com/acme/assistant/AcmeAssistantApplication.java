package com.acme.assistant;

import com.acme.assistant.infrastructure.config.AcmeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AcmeProperties.class)
@EnableScheduling
public class AcmeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcmeAssistantApplication.class, args);
    }
}
