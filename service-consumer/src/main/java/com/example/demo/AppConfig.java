package com.example.demo;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @LoadBalanced
    @Bean
    @Profile("!mesh")
    public RestTemplate restTemplateForSpringCloud() {
        return new RestTemplate();
    }

    @Bean
    @Profile("mesh")
    public RestTemplate restTemplateForMesh() {
        return new RestTemplate();
    }

}
