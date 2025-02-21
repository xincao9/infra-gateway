package fun.golinks.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
public class GatewayBlockRequestHandler implements BlockRequestHandler {

    private static final String BLOCK_MSG = "尊敬的用户，您好！\n" +
            "\n" +
            "由于您的访问频率过高，已触发网站的流量限制保护。这是为了确保所有用户都能公平、稳定地使用我们的服务而设置的安全机制。请您稍作调整后重试。";

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        log.info("Blocked by Gateway: ", ex);
        if (acceptsHtml(exchange)) {
            return htmlErrorResponse();
        }
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildErrorResult());
    }

    private Mono<ServerResponse> htmlErrorResponse() {
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .bodyValue(BLOCK_MSG);
    }

    private ErrorResult buildErrorResult() {
        return new ErrorResult(HttpStatus.TOO_MANY_REQUESTS.value(),
                BLOCK_MSG);
    }

    private boolean acceptsHtml(ServerWebExchange exchange) {
        try {
            List<MediaType> acceptedMediaTypes = exchange.getRequest().getHeaders().getAccept();
            acceptedMediaTypes.remove(MediaType.ALL);
            MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
            return acceptedMediaTypes.stream()
                    .anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    private static class ErrorResult {
        private final int code;
        private final String message;
    }
}
