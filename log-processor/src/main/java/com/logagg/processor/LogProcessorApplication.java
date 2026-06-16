package com.logagg.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // needed for the 1-minute window evaluation scheduler
public class LogProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogProcessorApplication.class, args);
    }
}
