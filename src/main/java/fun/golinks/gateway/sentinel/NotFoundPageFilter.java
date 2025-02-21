package fun.golinks.gateway.sentinel;

import com.alibaba.fastjson.JSON;
import fun.golinks.gateway.util.WebUtils;
import fun.golinks.gateway.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
public class NotFoundPageFilter implements GlobalFilter, Ordered {

    public static final String NOT_FOUND_MESSAGE = "页面未找到\n\n抱歉，您请求的页面不存在。请检查 URL 或稍后重试。";
    public static String NOT_FOUND_HTML = "";

    static {
        try {
            NOT_FOUND_HTML = new String(Files.readAllBytes(Paths.get("src/main/resources/static/404.html")));
        } catch (Throwable e) {
            log.warn("", e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.defer(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpStatus status = response.getStatusCode();
            if (status == HttpStatus.NOT_FOUND && !response.isCommitted()) {
                if (WebUtils.acceptsHtml(exchange)) {
                    response.setStatusCode(HttpStatus.NOT_FOUND);
                    byte[] body = NOT_FOUND_HTML.getBytes(Charset.defaultCharset());
                    response.getHeaders().add("Content-Type", "text/html;charset=UTF-8");
                    response.getHeaders().setContentLength(body.length);
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
                } else {
                    response.setStatusCode(HttpStatus.NOT_FOUND);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    ErrorResult errorResult = new ErrorResult(HttpStatus.NOT_FOUND.value(), NOT_FOUND_MESSAGE);
                    return response.writeWith(Mono.just(response.bufferFactory().wrap(JSON.toJSONBytes(errorResult))));
                }
            }
            return Mono.empty();
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // 设置较低优先级，确保在路由处理后执行
    }
}