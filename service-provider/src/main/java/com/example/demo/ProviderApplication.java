package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@RestController
@EnableDiscoveryClient
@RefreshScope
public class ProviderApplication {
	private static final Logger LOG = LoggerFactory.getLogger(ProviderApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ProviderApplication.class, args);
	}

	@Value(value = "${useLocalCache:false}")
	private boolean useLocalCache;

	@Autowired
	InetUtils inetUtils;

	@RequestMapping(value = "/echo/{param}", method = RequestMethod.GET)
	public String echo(@PathVariable String param) {
		LOG.info("provider-demo -- request param: [" + param + "]");
		String result = "请求参数: " + param + " on " + inetUtils.findFirstNonLoopbackAddress().getHostAddress() + "\n";

		return result;
	}

	@RequestMapping("/get")
	public boolean get() {
		return useLocalCache;
	}

}
