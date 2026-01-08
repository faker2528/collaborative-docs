package com.collab.history;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 历史服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.collab.history.mapper")
public class HistoryServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HistoryServiceApplication.class, args);
    }
}
