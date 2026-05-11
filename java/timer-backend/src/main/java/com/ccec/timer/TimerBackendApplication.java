package com.ccec.timer;

import com.ccec.timer.config.TimerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TimerProperties.class)
public class TimerBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimerBackendApplication.class, args);
    }
}
