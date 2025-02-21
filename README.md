# infra-gateway

基础架构-业务网关

## 服务发现配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos
        namespace: api
    gateway:
      discovery:
        locator:
          lower-case-service-id: true
          enabled: true
      routes:
        - id: sample-route
          uri: lb://sample
          predicates:
            - Path=/sample/**
          filters:
            - StripPrefix=1
```

## 限流配置

```json
[
  {
    "resource": "sample-route",
    "resourceMode": 0,
    "grade": 1,
    "count": 5,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0
  },
  {
    "resource": "sample-api",
    "resourceMode": 1,
    "grade": 1,
    "count": 10,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0
  }
]
```

* nacos配置位置namespace:sentinel；data-id: infra-gateway-flow-rules；group-id: infra-gateway
* resource：限流资源，可以是路由 ID（如 test_route）或自定义 API（如 test_api）。
* resourceMode：0 表示路由限流，1 表示自定义 API 限流。
* count：限流阈值（如 QPS 为 5 或 10）。
* intervalSec：统计时间窗口（单位：秒）。

## 监控（普罗米修斯）

```yaml
spring:
  cloud:
      metrics:
        enabled: true # 启用 Gateway Metrics
management:
  endpoints:
    web:
      exposure:
        include: "*" # 暴露所有端点
      base-path: /management
  metrics:
    export:
      prometheus:
        enabled: true # 启用 Prometheus 指标导出```

* 普罗米修斯拉取指标地址 http://localhost:10101/management/prometheus

## 资料

* [spring-cloud-gateway](https://docs.spring.io/spring-cloud-gateway/docs/3.1.9/reference/html/#gateway-starter)
* [https://start.aliyun.com/](https://start.aliyun.com/)