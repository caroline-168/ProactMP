package org.liuscraft.mydubbo.consumer;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @description: A proactive multipath transport protocol for low-latency datacenters
 * @Author: ProactMP (USTC)
 * @CreateTime: 2023-12-21 12:00
 */

@SpringBootApplication
@EnableDubbo
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}