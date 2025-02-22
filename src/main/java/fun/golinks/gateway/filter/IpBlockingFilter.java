package fun.golinks.gateway.filter;

import fun.golinks.gateway.util.JsonUtil;
import fun.golinks.gateway.util.WebUtils;
import fun.golinks.gateway.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
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
public class IpBlockingFilter implements GlobalFilter {

    private static final long MAX_VIOLATIONS = 5L; // 最大违规次数
    private static final Duration BAN_DURATION = Duration.ofMinutes(30); // 封禁时长
    private static final String FORBIDDEN_MESSAGE = "很抱歉，您暂时无法访问此页面。可能是由于身份验证失败或您的请求触发了安全限制。请检查您的登录凭证是否正确，或稍后再试。如果问题持续存在，请联系我们的支持团队获取帮助。感谢您的理解！";
    private static String FORBIDDEN_PAGE = FORBIDDEN_MESSAGE;

    static {
        try {
            FORBIDDEN_PAGE = new String(Files.readAllBytes(Paths.get("src/main/resources/static/forbidden.html")));
        } catch (Throwable e) {
            log.warn("", e);
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
        return redisTemplate.opsForValue().get("banned:" + clientIp).flatMap(banned -> {
            if (banned != null && banned.equals("true")) {
                return forbiddenResponse(exchange);
            }
            return increment(exchange, chain, clientIp);
        });
    }

    private Mono<Void> increment(ServerWebExchange exchange, GatewayFilterChain chain, String clientIp) {
        String requestCounterKey = "request:" + clientIp;
        return redisTemplate.opsForValue().increment(requestCounterKey).flatMap(count -> {
            if (count >= MAX_VIOLATIONS) {
                // 达到阈值，封禁 IP
                return redisTemplate.opsForValue().set("banned:" + clientIp, "true", BAN_DURATION)
                        .then(forbiddenResponse(exchange));
            }
            // 未达到阈值，返回错误但不封禁
            return forbiddenResponse(exchange);
        }).then(Mono.defer(() -> redisTemplate.expire(requestCounterKey, BAN_DURATION))).then(chain.filter(exchange)); // 设置过期时间
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
}