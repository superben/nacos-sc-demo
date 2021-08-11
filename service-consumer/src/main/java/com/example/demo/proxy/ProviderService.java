package com.example.demo.proxy;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * FeignClient for service-provider.
 *
 * Note: url的值是"${feign.client.url.provider:}"，而不是"${feign.client.url.provider}"，多了一个冒号，
 * 意思是如果没有feign.client.url.provider属性值，就用空。
 *
 * 而当url是空的时候，FeignClient会尝试从注册中心获取服务实例，进行负载均衡。而当url非空时，就不会进行负载均衡，
 * 我们利用此特性，让Mesh环境的sidecar做负载均衡。
 */
@FeignClient(name = "service-provider", url = "${feign.client.url.provider:}")
public interface ProviderService {
    @RequestMapping(value = "/echo/{str}", method = RequestMethod.GET)
    String echo(@PathVariable("str") String str);
}