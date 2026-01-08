package com.collab.collaboration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 协作服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CollaborationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CollaborationServiceApplication.class, args);
    }
}
