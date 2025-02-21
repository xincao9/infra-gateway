package fun.golinks.gateway.filter;

import com.alibaba.fastjson.JSON;
import fun.golinks.gateway.properties.JwtProperties;
import fun.golinks.gateway.vo.ErrorResult;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Resource
    private JwtProperties jwtProperties;
    private static final String AUTH_HEADER = "Authorization";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取 Authorization 头
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authHeader = headers.getFirst(AUTH_HEADER);
        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }
        // 提取 JWT
        String token = authHeader.substring(7); // 移除 "Bearer "
        Claims claims = jwtProperties.parseToken(token);
        if (claims == null) {
            return unauthorizedResponse(exchange);
        }
        // 将用户信息添加到请求头，传递给下游
        log.info("JWT validated successfully for user: {}", claims.getSubject());
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate().header("X-User-Id", claims.getSubject()) // 假设 subject 是用户 ID
                        .header("X-Roles", claims.get("roles", String.class)) // 假设 roles 在 JWT 中
                        .build())
                .build();
        return chain.filter(modifiedExchange);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ErrorResult errorResult = new ErrorResult(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT token");
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(JSON.toJSONBytes(errorResult));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // 优先级最高，确保认证最先执行
    }
}