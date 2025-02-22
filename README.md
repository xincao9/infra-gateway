# Infra-Gateway

一个开箱即用的业务网关服务，基于 **Spring Cloud Gateway** 和 **Nacos** 实现动态路由、流量控制和监控支持。

---

## 功能特性

- **动态路由**：通过 Nacos 配置中心实现路由规则的动态管理。
- **流量控制**：集成 Sentinel，支持基于路由和 API 的限流。
- **服务发现**：通过 Nacos Discovery 实现服务注册与路由。
- **监控支持**：集成 Prometheus，提供网关性能指标。

---

## 快速开始

### 1. 服务发现与配置

#### `application.yml`

基础配置，用于服务发现和静态路由定义。

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
          enabled: true                # 启用服务发现路由
          lower-case-service-id: true  # 服务 ID 小写化
      routes:
        - id: sample-route
          uri: lb://sample           # 通过服务名路由
          predicates:
            - Path=/sample/**
          filters:
            - StripPrefix=1
```

#### 动态路由配置（Nacos）

在 Nacos 中配置动态路由规则，支持实时更新。

- **Namespace**: `api`
- **Data ID**: `gateway-routes.yaml`
- **Group**: `infra-gateway`
- **格式**: JSON OR YAML

```yaml
- id: sample-route
  uri: lb://sample
  predicates:
  - Path=/sample/**
  filters:
  - StripPrefix=1
```

---

### 2. 流量控制（Sentinel）

通过 Sentinel 实现网关的限流配置，规则存储在 Nacos 中。

#### 配置示例

- **Namespace**: `sentinel`
- **Data ID**: `infra-gateway-flow-rules`
- **Group**: `infra-gateway`
- **格式**: JSON

```json
[
  {
    "resource": "sample-route",
    "resourceMode": 0,
    "grade": 1,
    "count": 1,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0
  },
  {
    "resource": "sample-api",
    "resourceMode": 1,
    "grade": 1,
    "count": 1,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0
  }
]
```

#### 参数说明

| 参数                | 说明                 | 可选值                                   |
|-------------------|--------------------|---------------------------------------|
| `resource`        | 限流资源（如路由 ID 或 API） | 自定义字符串                                |
| `resourceMode`    | 资源类型               | 0（路由）/ 1（API）                         |
| `grade`           | 限流类型               | 0 (线程数) / 1（QPS）                      |
| `count`           | 限流阈值               | 数值（如 5、10）                            |
| `intervalSec`     | 统计时间窗口（秒）          | 整数（如 1）                               |
| `controlBehavior` | 限流行为               | 0 (直接拒绝) / 1(预热) / 2(限流) / 3(预热 + 限流) |
| `burst`           | 突发流量容量             | 0（禁用）或正整数                             |

---

### 3. 监控（Prometheus）

集成 Prometheus 收集网关指标，便于性能监控。

#### `application.yml`

```yaml
spring:
  cloud:
    gateway:
      metrics:
        enabled: true            # 启用 Gateway Metrics
management:
  endpoints:
    web:
      exposure:
        include: "*"           # 暴露所有端点
      base-path: /management   # 自定义端点路径
  metrics:
    export:
      prometheus:
        enabled: true          # 启用 Prometheus 指标导出
```

#### 指标拉取地址

- **URL**: `http://localhost:10101/management/prometheus`
- **说明**: Prometheus 可通过此端点拉取指标，如请求延迟、吞吐量等。

---

## 使用步骤

1. **启动 Nacos**：确保 Nacos 服务运行在 `127.0.0.1:8848`，并创建 `api` 和 `sentinel` 命名空间。
2. **配置路由与限流**：在 Nacos 中添加上述配置。
3. **运行网关**：启动 Spring Boot 应用。
4. **测试路由**：访问 `http://localhost:10101/sample/xxx`，验证路由和限流是否生效。
5. **监控指标**：配置 Prometheus 拉取指标，并结合 Grafana 可视化。

---

## 依赖与参考

- **Spring Cloud Gateway
  **: [官方文档](https://docs.spring.io/spring-cloud-gateway/docs/3.1.9/reference/html/#gateway-starter)
- **Nacos**: [阿里云快速开始](https://start.aliyun.com/)
- **Sentinel**: [GitHub](https://github.com/alibaba/Sentinel)
- **Prometheus**: [官方文档](https://prometheus.io/docs/introduction/overview/)

---

## 注意事项

- **版本兼容性**：确保 Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 版本匹配。
- **安全性**：生产环境中为 Nacos 和 Prometheus 端点添加认证。
- **动态刷新**：路由和限流规则修改后会自动生效，无需重启服务。