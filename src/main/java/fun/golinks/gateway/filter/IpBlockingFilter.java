package fun.golinks.gateway.filter;

import fun.golinks.gateway.util.JsonUtil;
import fun.golinks.gateway.util.WebUtils;
import fun.golinks.gateway.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

@Slf4j
@Component
public class IpBlockingFilter implements GlobalFilter, Ordered {

    private static final long MAX_REQUEST_COUNT = 60L; // 最大请求次数
    private static final Duration BAN_DURATION = Duration.ofMinutes(30); // 封禁时长
    private static final String FORBIDDEN_MESSAGE = "很抱歉，您暂时无法访问此页面。可能是由于身份验证失败或您的请求触发了安全限制。请检查您的登录凭证是否正确，或稍后再试。如果问题持续存在，请联系我们的支持团队获取帮助。感谢您的理解！";
    private static String FORBIDDEN_PAGE = FORBIDDEN_MESSAGE;

    static {
        try {
            FORBIDDEN_PAGE = new String(Files.readAllBytes(Paths.get("src/main/resources/static/forbidden.html")));
        } catch (Throwable e) {
            log.warn("Failed to load forbidden page, using default message", e);
        }
    }

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public IpBlockingFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = WebUtils.getClientIp(exchange);
        // 检查 IP 是否被封禁
        return redisTemplate.opsForValue().get("banned:" + clientIp).defaultIfEmpty("false").flatMap(banned -> {
            if ("true".equals(banned)) {
                return forbiddenResponse(exchange);
            }
            return increment(exchange, chain, clientIp);
        }).onErrorResume(e -> {
            log.error("Redis operation failed", e);
            return chain.filter(exchange);
        });
    }

    private Mono<Void> increment(ServerWebExchange exchange, GatewayFilterChain chain, String clientIp) {
        String requestCounterKey = "request:" + clientIp;
        return redisTemplate.opsForValue().increment(requestCounterKey).flatMap(count -> {
            if (count == 1) {
                // 第一次请求，设置过期时间
                return redisTemplate.expire(requestCounterKey, Duration.ofMinutes(1)).then(chain.filter(exchange));
            } else if (count >= MAX_REQUEST_COUNT) {
                // 达到阈值，封禁 IP
                return redisTemplate.opsForValue().set("banned:" + clientIp, "true", BAN_DURATION)
                        .then(forbiddenResponse(exchange));
            }
            return chain.filter(exchange);
        });
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        if (WebUtils.acceptsHtml(exchange)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            byte[] body = FORBIDDEN_PAGE.getBytes(Charset.defaultCharset());
            response.getHeaders().add("Content-Type", "text/html;charset=UTF-8");
            response.getHeaders().setContentLength(body.length);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } else {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            ErrorResult errorResult = new ErrorResult(HttpStatus.FORBIDDEN.value(), FORBIDDEN_MESSAGE);
            return response
                    .writeWith(Mono.just(response.bufferFactory().wrap(JsonUtil.toJson(errorResult).getBytes())));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
