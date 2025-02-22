package fun.golinks.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "trace-id"; // 自定义 trace-id 头名称
    private static final String START_TIME_ATTR = "start-time"; // 用于存储开始时间的属性名

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        String method = request.getMethodValue();
        HttpHeaders headers = request.getHeaders();
        String queryParams = request.getQueryParams().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                .collect(Collectors.joining("&"));

        // 获取请求头中的 trace-id
        String traceId = headers.getFirst(TRACE_ID_HEADER);
        // 如果没有 trace-id，生成一个新的
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(START_TIME_ATTR, startTime);

        // 将 trace-id 添加到下游请求头
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate().header(TRACE_ID_HEADER, traceId).build()).build();

        log.info("Request received: method={}, uri={}, queryParams={}", method, uri, queryParams);
        // 继续过滤器链
        return chain.filter(modifiedExchange).doFinally(signalType -> {
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime; // 转换为毫秒
            log.info("Request completed: method={}, uri={}, status={}, duration={}ms", method, uri,
                    exchange.getResponse().getStatusCode(), costTime);
            MDC.remove(TRACE_ID_HEADER); // 只移除 trace-id
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 设置最高优先级，确保最先执行
    }
}