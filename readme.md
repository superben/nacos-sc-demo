
# Nacos-SpringCloud-Mesh-demo
本项目用于展示如何将 Nacos+SpringCloud 应用从Kubernetes运行切换到Istio。

Kubernetes模式下，使用Nacos作为注册中心及配置中心；RestTemplate使用K8s service做负载均衡。FeignClient从Nacos注册中心读取服务实例信息做负载均衡。

Istio模式下，Nacos只作为配置中心，Kubernetes作为注册中心，RestTemplate和FeignClient都使用sidecar做负载均衡。

## 编译环境 
JDK: AdoptOpenJDK-11.0.11+9

Base Image: openjdk:11-jre-slim

Nacos Image: nacos/nacos-server:2.0.3-slim

## 构建项目
分别进入service-provider及service-consumer子目录，执行如下命令：
```
# 编译打包，上传镜像
mvn clean package; docker build -t 10.200.10.1:5000/demo/provider:v1.0 .; docker push 10.200.10.1:5000/demo/provider:v1.0

# 编译打包，上传镜像
mvn clean package; docker build -t 10.200.10.1:5000/demo/consumer:v1.0 .; docker push 10.200.10.1:5000/demo/consumer:v1.0
```

## 实现细节
两种实现方式:
- service-consumer: SPRING_PROFILES_ACTIVE环境变量切换应用profile
- service-provider: SPRING_CLOUD_ENABLED环境变量切换spring.cloud.nacos.discovery.enabled

实际项目应用，请采用SPRING_PROFILES_ACTIVE环境变量切换应用profile方式。

### 使用SPRING_CLOUD_ENABLED环境变量切换Kubernetes模式和Istio模式
- 详见service-provider工程的bootstrap.yml
```
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_PORT:127.0.0.1:8848}
        enabled: ${SPRING_CLOUD_ENABLED:false}
```

### 使用SPRING_PROFILES_ACTIVE环境变量切换应用profile
#### 如何切换应用profile
```
# 启用Mesh模式
export SPRING_PROFILES_ACTIVE=mesh

# 启用Spring Cloud模式，即使用Nacos作为注册中心
export SPRING_PROFILES_ACTIVE=
```

- 详见service-consumer工程的application-mesh.yml
```
spring:
  cloud:
    nacos:
      discovery:
        enabled: false

feign:
  client:
    url:
      provider: http://service-provider:8080  # Mesh环境下，指定feign client的url属性，意味着sidecar将负责负载均衡
```

#### 切换RestTemplate bean实例
- 详见service-consumer工程的AppConfig.java
```
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
```

#### FeignClient实例
- 详见service-consumer工程的
  - ProviderService.java
```
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
```


注：
- Mesh模式下，不要使用FeignClient的name模式
- 使用FeignClient的url模式的同时，注意URL要加上端口号，才能同时兼容Kubernetes和Istio。

## 运行项目
本项目提供如下YAML文件:
- k8s.yml for K8s
- istio.yml for istio

```
# 检查命名空间的边车模式
kubectl get --show-labels ns
```

### Kubernetes
```
# 切换到Isto环境的default命名空间到非边车模式
kubectl label namespace default istio-injection-

# ka k8s.yml 
kubectl apply -f k8s.yml 

# krm -f k8s.yml
kubectl delete -f k8s.yml
```

### Istio
```
# 切换到Isto环境的default命名空间到边车模式
kubectl label namespace default istio-injection=enabled

# ka istio.yml 
kubectl apply -f istio.yml 

# krm -f istio.yml
kubectl delete -f istio.yml
```

### 验证结果

- 首先验证Nacos及Provider：
```
# 执行krun/kex，进入busybox-curl容器内
krun  busybox --image=yauritux/busybox-curl
kex busybox -- sh

######################
## 以下命令都从容器内执行：
######################
# 检查service-provider及service-consumer是否已经注册。当非边车模式时，会展示相应实例。
curl nacos:8848/nacos/v1/ns/instance/list?serviceName=service-provider
curl nacos:8848/nacos/v1/ns/instance/list?serviceName=service-consumer

# 检查useLocalCache的值，默认为false，当更新后为true
curl service-provider:8080/get

# 更新应用参数useLocalCache为true，其默认值为false。
curl -X POST "http://nacos:8848/nacos/v1/cs/configs?dataId=service-provider&group=DEFAULT_GROUP&content=useLocalCache=true"

# 检查provider是否正常 
curl service-provider:8080/echo/2019

```

- 验证Consumer
```
######################
## 以下命令都从容器内执行：
######################
# 通过RestTemplate方式调用provider
curl service-consumer:8081/echo-rest/2021

# 通过FeignClient的url方式调用provider
curl service-consumer:8081/echo-feign-url/2022

# 通过FeignClient的name方式调用provider
curl service-consumer:8081/echo-feign/2020
```

## 结果分析
### FeignClient本地缓存的影响
- Spring Cloud模式下，FeignClient的url方式有本地缓存，所以看到service-consumer:8081/echo-feign-url的结果不进行负载均衡。
- Istio模式下，FeignClient的url方式虽然有本地缓存，但是service-consumer:8081/echo-feign-url的结果是负载均衡的。说明sidecar负责的均衡的。
### FeignClient的name模式
- Spring Cloud模式下，由于使用Nacos作为注册中心，consumer的FeignClient可以从Nacos注册中心获得provider的实例，所以调用正常
- Istio模式下，由于使用Kubernetes作为注册中心，consumer的FeignClient获取不到provider的实例，所以调用失败。
### 我们的FeignClient实现方式
- Spring Cloud模式下，虽然使用FeignClient的url方式，但url值为空，FeignClient还会Nacos注册中心获得provider的实例，正常进行负载均衡
- Istio模式下，为FeignClient的url方式指定url值，由sidecar进行负载均衡
### RestTemplate模式
- Spring Cloud模式下，由于使用Nacos作为注册中心，使用@LoadBalanced注解，获取provider的实例，由RestTemplate负责负载均衡。
- Istio模式下，使用Kubernetes作为注册中心，使用常规RestTemplate实例，使用Kubernetes Service名访问provider，由sidecar负责负载均衡。

## 附录
### 本地调试命令集

```
docker build -t demo/provider:v1.0 .
docker run --rm -p 8080:8080 --env NACOS_SERVER_PORT=192.168.1.131:8848 demo/provider:v1.0
```

### 本地docker-compose调试
本项目调试初期使用docker-compose调试，需要Nacos和provider&consumer应用启动时间匹配，所以使用两个docker-cmopose文件启动。

``` 
docker-compose up
docker-compose -f services.yml up
```

### 指定工程profile
```
see: https://stackoverflow.com/questions/40060989/how-to-use-spring-boot-profiles

# 方式1
export SPRING_PROFILES_ACTIVE=mesh
mvn clean spring-boot:run 

# 方式2
mvn clean spring-boot:run -Dspring-boot.run.profiles=mesh

# 方式3
java -jar target/app.jar --spring.profiles.active=mesh

# 方式4
java -jar -Dspring.profiles.active=mesh target/app.jar
```