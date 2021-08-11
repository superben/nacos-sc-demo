package com.example.demo;

import com.example.demo.proxy.ProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
@EnableDiscoveryClient
@RefreshScope
@EnableFeignClients
public class ConsumerApplication {
	private static final Logger LOG = LoggerFactory.getLogger(ConsumerApplication.class);

	private final RestTemplate restTemplate;

	@Autowired
	private ProviderService providerService;

	@Autowired
	public ConsumerApplication(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(ConsumerApplication.class, args);
	}

	@RequestMapping(value = "/echo-rest/{param}", method = RequestMethod.GET)
	public String echo(@PathVariable String param) {
		LOG.info("consumer-demo -- request param: [" + param + "]");
		return restTemplate.getForObject("http://service-provider:8080/echo/" + param, String.class);
	}

	@RequestMapping(value = "/echo-feign/{str}", method = RequestMethod.GET)
	public String feignProvider(@PathVariable String str) {
		return providerService.echo(str);
	}

}
