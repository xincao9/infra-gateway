# infra-gateway

基础架构-业务网关

## 限流配置

```json
[
    {
        "resource": "sample_route",
        "resourceMode": 0,
        "grade": 1,
        "count": 5,
        "intervalSec": 1,
        "controlBehavior": 0,
        "burst": 0
    },
    {
        "resource": "sample_api",
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

## 资料

* [spring-cloud-gateway](https://docs.spring.io/spring-cloud-gateway/docs/3.1.9/reference/html/#gateway-starter)
* [https://start.aliyun.com/](https://start.aliyun.com/)