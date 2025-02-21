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
import java.util.Date;
import java.util.Map;

/**
 * <pre>
 *     curl -X GET -H 'Authorization:Bearer eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0..ERG6yVlTSS3pnLdS.1nDMIk3BxPcBJOZOpdgNAz12d7RBdb0rj4_NXeV7svK2Spar66DoOPQieCyY9nFQEPRCCaHohcxWzePCm9SdTnDc.jXdHINr-Lb-T5hB5aWH86w' 'http://localhost:10100/sample/github/contributors'
 * </pre>
 */
@Slf4j
@Component
@SuppressWarnings("all")
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Resource
    private JwtProperties jwtProperties;
    private static final String AUTH_HEADER = "Authorization";

    /**
     * 过滤器方法，用于处理请求的授权验证和用户信息传递。
     * 该方法会检查请求头中的 Authorization 字段，验证 JWT 的有效性，
     * 并将 JWT 中的用户信息添加到请求头中，传递给下游服务。
     *
     * @param exchange ServerWebExchange 对象，包含当前请求和响应的上下文信息。
     * @param chain    GatewayFilterChain 对象，用于继续执行过滤器链中的下一个过滤器。
     * @return Mono<Void> 表示异步处理的结果，可能包含未授权的响应或继续执行过滤器链。
     */
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求头中的 Authorization 字段
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authHeader = headers.getFirst(AUTH_HEADER);

        // 如果 Authorization 头为空或不以 "Bearer " 开头，则直接继续执行过滤器链
        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        // 从 Authorization 头中提取 JWT 令牌
        String token = authHeader.substring(7); // 移除 "Bearer " 前缀

        // 解析 JWT 令牌并获取其中的声明信息
        Claims claims = jwtProperties.parseToken(token);
        if (claims == null) {
            // 如果解析失败，返回未授权的响应
            return unauthorizedResponse(exchange);
        }

        // 检查 JWT 是否已过期
        Date expiration = claims.getExpiration();
        if (expiration != null && new Date().after(expiration)) {
            // 如果 JWT 已过期，返回未授权的响应
            return unauthorizedResponse(exchange);
        }

        // 将 JWT 中的用户信息添加到请求头中，传递给下游服务
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(httpHeaders -> {
                            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                                httpHeaders.add(entry.getKey(), String.valueOf(entry.getValue()));
                            }
                        })
                        .build())
                .build();

        // 继续执行过滤器链，传递修改后的请求
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