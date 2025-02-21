package fun.golinks.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import fun.golinks.gateway.util.WebUtils;
import fun.golinks.gateway.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class GatewayBlockRequestHandler implements BlockRequestHandler {

    private static final String BLOCK_MSG = "尊敬的用户，您好！\n" + "\n"
            + "由于您的访问频率过高，已触发网站的流量限制保护。这是为了确保所有用户都能公平、稳定地使用我们的服务而设置的安全机制。请您稍作调整后重试。";

    private static String BLOCK_HTML = "";

    static {
        try {
            BLOCK_HTML = new String(Files.readAllBytes(Paths.get("src/main/resources/static/404.html")));
        } catch (Throwable e) {
            log.warn("", e);
        }
    }

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        if (WebUtils.acceptsHtml(exchange)) {
            return htmlErrorResponse();
        }
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildErrorResult());
    }

    private Mono<ServerResponse> htmlErrorResponse() {
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).header("Content-Type", "text/html;charset=UTF-8")
                .bodyValue(BLOCK_HTML);
    }

    private ErrorResult buildErrorResult() {
        return new ErrorResult(HttpStatus.TOO_MANY_REQUESTS.value(), BLOCK_MSG);
    }

}
