package com.example.demo;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
public class AppConfig {

    @LoadBalanced
    @Bean
    @ConditionalOnProperty(
        value = {"spring.cloud.nacos.discovery.enabled"},
        havingValue = "true"
    )
    public RestTemplate restTemplateForSpringCloud() {
        return new RestTemplate();
    }


    @Bean
    @ConditionalOnProperty(
        value = {"spring.cloud.nacos.discovery.enabled"},
        havingValue = "false"
    )
    public RestTemplate restTemplateForMesh() {
        return new RestTemplate();
    }

}
